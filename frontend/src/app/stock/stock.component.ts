import { Component, OnInit } from '@angular/core';
import { BehaviorSubjectService } from '../behavior-subject.service';
import { WebSocketService } from '../web-socket.service';

@Component({
  selector: 'app-stock',
  templateUrl: './stock.component.html',
  styleUrls: ['./stock.component.scss']
})
export class StockComponent implements OnInit {

  constructor(private behaviorSubjectService: BehaviorSubjectService, private webSockerService: WebSocketService) { }

  ngOnInit() {
  }

}
