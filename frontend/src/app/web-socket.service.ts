import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import 'rxjs/add/observable/timer';
import { BehaviorSubjectService } from './behavior-subject.service';
import { Portfolio } from './model';

@Injectable() export class WebSocketService {
  ws: WebSocket;

  constructor(private behaviorSubjectService: BehaviorSubjectService) {
    setTimeout(() => {
      const url = new URL(location.href);
      this.ws = new WebSocket('ws://' + url.hostname + ':' + url.port + '/api/websocket');
      this.ws.onopen = (event) => console.log('socket opened');
      this.ws.onclose = (event) => console.log('socket closed');
      this.ws.onmessage = this.onmessage;
      this.ws.onerror = (event) => console.log('socket error ' + JSON.stringify(event));
    }, 1904);
  }

  sendMessage(message: string) {
    if (this.ws) this.ws.send(message);
  }

  private onmessage(event: MessageEvent) {
    const s = event.data;
    if (s.match(/^PORTFOLIO: {/)) {
      try {
        const portfolio: Portfolio = JSON.parse(s.replace('PORTFOLIO: {', '{'));
        this.behaviorSubjectService.portfolioSource.next(portfolio);
      }
      catch (e) { /* who cares */ }
    }
  }
}
