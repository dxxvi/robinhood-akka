import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';

import { AppComponent } from './app.component';
import { StockComponent } from './stock/stock.component';
import { BehaviorSubjectService } from './behavior-subject.service';
import { WebSocketService } from './web-socket.service';

@NgModule({
  declarations: [
    AppComponent,
    StockComponent
  ],
  imports: [
    BrowserModule
  ],
  providers: [
    BehaviorSubjectService,
    WebSocketService
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
