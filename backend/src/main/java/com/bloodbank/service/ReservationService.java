package com.bloodbank.service;

import com.bloodbank.dto.CreateReservationRequest;
import com.bloodbank.dto.ReservationDto;
import com.bloodbank.entity.BloodBank;
import com.bloodbank.entity.Reservation;
import com.bloodbank.repository.BloodBankRepository;
import com.bloodbank.repository.BloodInventoryRepository;
import com.bloodbank.repository.ReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);

    private final ReservationRepository reservationRepository;
    private final BloodBankRepository bloodBankRepository;
    private final BloodInventoryRepository inventoryRepository;
    private final WhatsAppService whatsAppService;

    public ReservationService(ReservationRepository reservationRepository,
            BloodBankRepository bloodBankRepository,
            BloodInventoryRepository inventoryRepository,
            WhatsAppService whatsAppService) {
        this.reservationRepository = reservationRepository;
        this.bloodBankRepository = bloodBankRepository;
        this.inventoryRepository = inventoryRepository;
        this.whatsAppService = whatsAppService;
    }

    @Transactional
    public ReservationDto createReservation(CreateReservationRequest request) {
        BloodBank bloodBank = bloodBankRepository.findById(request.getBloodBankId())
                .orElseThrow(() -> new RuntimeException("Blood bank not found"));

        // Validate that requested units don't exceed available inventory
        int availableUnits = inventoryRepository.findByBloodBankIdAndBloodType(
                request.getBloodBankId(), request.getBloodType())
                .map(inv -> inv.getUnitsAvailable())
                .orElse(0);

        if (request.getUnitsNeeded() > availableUnits) {
            throw new RuntimeException(
                    String.format("Insufficient blood units. Requested: %d, Available: %d",
                            request.getUnitsNeeded(), availableUnits));
        }

        String whatsappNumber = "+91" + request.getWhatsappNumber().replaceAll("[^0-9]", "");

        Reservation reservation = Reservation.builder()
                .patientName(request.getPatientName())
                .whatsappNumber(whatsappNumber)
                .bloodType(request.getBloodType())
                .unitsNeeded(request.getUnitsNeeded())
                .urgencyLevel(request.getUrgencyLevel() != null ? request.getUrgencyLevel() : "normal")
                .additionalNotes(request.getAdditionalNotes())
                .bloodBank(bloodBank)
                .status(Reservation.STATUS_PENDING)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();

        // Set prescription and doctor (required fields)
        reservation.setPrescriptionPath(request.getPrescriptionPath());
        reservation.setReferringDoctor(request.getReferringDoctor());

        Reservation saved = reservationRepository.save(reservation);
        log.info("New reservation created: {}", saved.getId());

        ReservationDto dto = convertToDto(saved, bloodBank.getName());

        try {
            whatsAppService.sendReservationConfirmation(dto);
        } catch (Exception e) {
            log.warn("Failed to send WhatsApp notification: {}", e.getMessage());
        }

        return dto;
    }

    public List<ReservationDto> getAllReservations() {
        return reservationRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(r -> convertToDto(r, r.getBloodBank().getName()))
                .collect(Collectors.toList());
    }

    public Optional<ReservationDto> getReservationById(Long id) {
        return reservationRepository.findById(id)
                .map(r -> convertToDto(r, r.getBloodBank().getName()));
    }

    public List<ReservationDto> getReservationsByBankId(Long bankId) {
        return reservationRepository.findByBloodBankIdOrderByCreatedAtDesc(bankId).stream()
                .map(r -> convertToDto(r, r.getBloodBank().getName()))
                .collect(Collectors.toList());
    }

    @Transactional
    public Optional<ReservationDto> updateStatus(Long id, String status) {
        return reservationRepository.findById(id)
                .map(reservation -> {
                    String previousStatus = reservation.getStatus();
                    reservation.setStatus(status);
                    Reservation updated = reservationRepository.save(reservation);

                    log.info("Reservation {} status updated to {}", id, status);

                    // Deduct inventory when reservation is completed
                    if (Reservation.STATUS_COMPLETED.equals(status) &&
                            !Reservation.STATUS_COMPLETED.equals(previousStatus)) {
                        inventoryRepository.deductUnits(
                                reservation.getBloodBank().getId(),
                                reservation.getBloodType(),
                                reservation.getUnitsNeeded());
                        log.info("Deducted {} units of {} from bank {}",
                                reservation.getUnitsNeeded(),
                                reservation.getBloodType(),
                                reservation.getBloodBank().getId());
                    }

                    try {
                        whatsAppService.sendStatusUpdate(
                                updated.getWhatsappNumber(),
                                updated.getPatientName(),
                                status,
                                updated.getBloodBank().getName());
                    } catch (Exception e) {
                        log.warn("Failed to send WhatsApp status update: {}", e.getMessage());
                    }

                    return convertToDto(updated, updated.getBloodBank().getName());
                });
    }

    @Transactional
    public Optional<ReservationDto> updateStatusForBank(Long reservationId, Long bankId, String status) {
        return reservationRepository.findById(reservationId)
                .filter(r -> r.getBloodBank().getId().equals(bankId))
                .map(reservation -> {
                    reservation.setStatus(status);
                    Reservation updated = reservationRepository.save(reservation);

                    if (Reservation.STATUS_COMPLETED.equals(status)) {
                        inventoryRepository.deductUnits(
                                bankId,
                                reservation.getBloodType(),
                                reservation.getUnitsNeeded());
                        log.info("Deducted {} units of {} from bank {}",
                                reservation.getUnitsNeeded(),
                                reservation.getBloodType(),
                                bankId);
                    }

                    try {
                        whatsAppService.sendStatusUpdate(
                                updated.getWhatsappNumber(),
                                updated.getPatientName(),
                                status,
                                updated.getBloodBank().getName());
                    } catch (Exception e) {
                        log.warn("Failed to send WhatsApp status update: {}", e.getMessage());
                    }

                    return convertToDto(updated, updated.getBloodBank().getName());
                });
    }

    @Transactional
    public Optional<ReservationDto> cancelReservation(Long id) {
        return reservationRepository.findById(id)
                .map(reservation -> {
                    reservation.setStatus(Reservation.STATUS_CANCELLED);
                    Reservation updated = reservationRepository.save(reservation);
                    return convertToDto(updated, updated.getBloodBank().getName());
                });
    }

    public Object[] getStats() {
        return reservationRepository.getReservationStats();
    }

    public Object[] getStatsByBankId(Long bankId) {
        return reservationRepository.getReservationStatsByBankId(bankId);
    }

    public List<ReservationDto> getRecentByBankId(Long bankId) {
        return reservationRepository.findTop5ByBloodBankIdOrderByCreatedAtDesc(bankId).stream()
                .map(r -> convertToDto(r, r.getBloodBank().getName()))
                .collect(Collectors.toList());
    }

    private ReservationDto convertToDto(Reservation reservation, String bloodBankName) {
        ReservationDto dto = ReservationDto.builder()
                .id(reservation.getId())
                .patientName(reservation.getPatientName())
                .whatsappNumber(reservation.getWhatsappNumber())
                .bloodType(reservation.getBloodType())
                .unitsNeeded(reservation.getUnitsNeeded())
                .urgencyLevel(reservation.getUrgencyLevel())
                .additionalNotes(reservation.getAdditionalNotes())
                .bloodBankId(reservation.getBloodBank().getId())
                .bloodBankName(bloodBankName)
                .status(reservation.getStatus())
                .createdAt(reservation.getCreatedAt())
                .expiresAt(reservation.getExpiresAt())
                .updatedAt(reservation.getUpdatedAt())
                .build();
        dto.setPrescriptionPath(reservation.getPrescriptionPath());
        dto.setReferringDoctor(reservation.getReferringDoctor());
        return dto;
    }
}
