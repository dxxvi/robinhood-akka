import { Component, OnInit } from '@angular/core';
import { BehaviorSubjectService } from './behavior-subject.service';
import { WebSocketService } from './web-socket.service';
import {Portfolio} from "./model";

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit {
  portfolio: Portfolio;

  // we dont use the websocket service here, but we have to inject it to here so that it can be initialized
  constructor(private bsService: BehaviorSubjectService, private wsService: WebSocketService) { }

  ngOnInit() {
    this.bsService.portfolioObservable.subscribe(p => this.portfolio = p);
  }

  getEquityString(): string {
    if (this.portfolio) {
      if (this.portfolio.extendedHoursEquity) {
        return AppComponent.formatNumber(this.portfolio.extendedHoursEquity);
      }
      else {
        return AppComponent.formatNumber(this.portfolio.equity);
      }
    }
    else {
      return ' '.repeat(10);
    }
  }

  getMarketValueString(): string {
    if (this.portfolio && this.portfolio.marketValue) {
      return AppComponent.formatNumber(this.portfolio.marketValue);
    }
    else {
      return ' '.repeat(10);
    }
  }

  getBuyPowerString(): string {
    if (this.portfolio && this.portfolio.marketValue) {
      if (this.portfolio.extendedHoursEquity) {
        return AppComponent.formatNumber(this.portfolio.extendedHoursEquity - this.portfolio.marketValue);
      }
      if (this.portfolio.equity) {
        return AppComponent.formatNumber(this.portfolio.equity - this.portfolio.marketValue);
      }
    }
    return ' '.repeat(10);
  }

  // 5 digits before the decimal point, 4 digits after
  private static formatNumber(n: number): string {
    if (n === null) {
      return ' '.repeat(10);
    }
    const a: Array<string> = ('' + n).split('.');
    if (a.length === 1) {
      return ' '.repeat(5 - a[0].length) + a[0] + ' '.repeat(5);
    }
    return ' '.repeat(5 - a[0].length) + a[0] + '.' + a[1] + ' '.repeat(4 - a[1].length);
  }

  // returns a string of length `l` with spaces padded to the left/right
  private static formatString(s: string, l: number, leftPad: boolean): string {
    if (s) {
      let n = l - s.length;
      if (n < 0) {
        n = 0;
      }
      return leftPad ? ' '.repeat(n) + s : s + ' '.repeat(n)
    }
    else {
      return ' '.repeat(l);
    }
  }
}
