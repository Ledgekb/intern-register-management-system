import { Component, signal } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { Navbar } from './shared/navbar/navbar';
import { HelpModalComponent } from './shared/components/help-modal/help-modal.component';
import { NgIf } from '@angular/common';
import { WebSocketService } from './services/websocket.service';
import { HelpService } from './services/help.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Navbar, HelpModalComponent, NgIf],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  constructor(
    private router: Router,
    private webSocketService: WebSocketService,
    private helpService: HelpService
  ) {
    console.log('✅ App component constructed');
    console.log('Initial route:', this.router.url);

    // Connect to WebSocket
    this.webSocketService.connect();
  }
  protected readonly title = signal('Intern-Register-System');

  // Hide on login, register & forced password change routes
  isAuthPage(): boolean {
    const currentRoute = this.router.url;
    return currentRoute.includes('login') || 
           currentRoute.includes('sign-up') || 
           currentRoute.includes('force-password-change');
  }

  toggleHelp(): void {
    this.helpService.toggleHelp();
  }
}
