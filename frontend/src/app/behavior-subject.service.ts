import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';
import { Portfolio } from './model';

@Injectable() export class BehaviorSubjectService {
  portfolioSource = new BehaviorSubject<Portfolio>({equity: null, extendedHoursEquity: null, marketValue: null});
  portfolioObservable = this.portfolioSource.asObservable();

  constructor() { }

}
