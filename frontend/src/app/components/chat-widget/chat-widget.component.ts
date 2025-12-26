import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';

interface ChatMessage {
    role: 'user' | 'assistant';
    content: string;
    timestamp: Date;
}

@Component({
    selector: 'app-chat-widget',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './chat-widget.component.html',
    styleUrls: ['./chat-widget.component.css']
})
export class ChatWidgetComponent {
    isOpen = false;
    messages: ChatMessage[] = [];
    userInput = '';
    isLoading = false;

    constructor(private http: HttpClient) {
        // Add welcome message
        this.messages.push({
            role: 'assistant',
            content: 'Hi! 👋 I\'m your Blood Donation Assistant. Ask me anything about blood donation, eligibility, or blood type compatibility!',
            timestamp: new Date()
        });
    }

    toggleChat() {
        this.isOpen = !this.isOpen;
    }

    sendMessage() {
        if (!this.userInput.trim() || this.isLoading) return;

        const userMessage = this.userInput.trim();
        this.userInput = '';

        // Add user message
        this.messages.push({
            role: 'user',
            content: userMessage,
            timestamp: new Date()
        });

        this.isLoading = true;

        // Call API
        this.http.post<any>('https://bloodbank-backend-701641288198.asia-south1.run.app/api/chat', { message: userMessage })
            .subscribe({
                next: (response) => {
                    this.messages.push({
                        role: 'assistant',
                        content: response.response,
                        timestamp: new Date()
                    });
                    this.isLoading = false;
                    this.scrollToBottom();
                },
                error: (err) => {
                    console.error('Chat error:', err);
                    this.messages.push({
                        role: 'assistant',
                        content: 'Sorry, I\'m having trouble responding right now. Please try again later.',
                        timestamp: new Date()
                    });
                    this.isLoading = false;
                }
            });

        this.scrollToBottom();
    }

    private scrollToBottom() {
        setTimeout(() => {
            const chatBody = document.querySelector('.chat-body');
            if (chatBody) {
                chatBody.scrollTop = chatBody.scrollHeight;
            }
        }, 100);
    }

    handleKeyPress(event: KeyboardEvent) {
        if (event.key === 'Enter' && !event.shiftKey) {
            event.preventDefault();
            this.sendMessage();
        }
    }
}
