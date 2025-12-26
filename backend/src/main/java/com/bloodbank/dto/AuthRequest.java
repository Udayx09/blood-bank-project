package com.bloodbank.dto;

public class AuthRequest {

    // Nested classes for auth requests/responses

    public static class RegisterRequest {
        private String phone;
        private String password;
        private Long bloodBankId;

        public RegisterRequest() {
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public Long getBloodBankId() {
            return bloodBankId;
        }

        public void setBloodBankId(Long bloodBankId) {
            this.bloodBankId = bloodBankId;
        }
    }

    public static class LoginRequest {
        private String phone;
        private String password;

        public LoginRequest() {
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class AuthResponse {
        private boolean success;
        private String message;
        private String token;
        private BankInfo bank;

        public AuthResponse() {
        }

        public AuthResponse(boolean success, String message, String token, BankInfo bank) {
            this.success = success;
            this.message = message;
            this.token = token;
            this.bank = bank;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public BankInfo getBank() {
            return bank;
        }

        public void setBank(BankInfo bank) {
            this.bank = bank;
        }

        public static AuthResponseBuilder builder() {
            return new AuthResponseBuilder();
        }

        public static class AuthResponseBuilder {
            private boolean success;
            private String message;
            private String token;
            private BankInfo bank;

            public AuthResponseBuilder success(boolean success) {
                this.success = success;
                return this;
            }

            public AuthResponseBuilder message(String message) {
                this.message = message;
                return this;
            }

            public AuthResponseBuilder token(String token) {
                this.token = token;
                return this;
            }

            public AuthResponseBuilder bank(BankInfo bank) {
                this.bank = bank;
                return this;
            }

            public AuthResponse build() {
                return new AuthResponse(success, message, token, bank);
            }
        }

        public static class BankInfo {
            private Long id;
            private String name;
            private String phone;

            public BankInfo() {
            }

            public BankInfo(Long id, String name, String phone) {
                this.id = id;
                this.name = name;
                this.phone = phone;
            }

            public Long getId() {
                return id;
            }

            public void setId(Long id) {
                this.id = id;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public String getPhone() {
                return phone;
            }

            public void setPhone(String phone) {
                this.phone = phone;
            }

            public static BankInfoBuilder builder() {
                return new BankInfoBuilder();
            }

            public static class BankInfoBuilder {
                private Long id;
                private String name;
                private String phone;

                public BankInfoBuilder id(Long id) {
                    this.id = id;
                    return this;
                }

                public BankInfoBuilder name(String name) {
                    this.name = name;
                    return this;
                }

                public BankInfoBuilder phone(String phone) {
                    this.phone = phone;
                    return this;
                }

                public BankInfo build() {
                    return new BankInfo(id, name, phone);
                }
            }
        }
    }
}
