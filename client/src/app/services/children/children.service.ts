import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { Response } from '@angular/http';
import { HttpApiClient } from '../http/http-api-client.service';
import { Child } from '../../models/child'
import { Preferences } from '../../models/preferences'

@Injectable()
export class ChildrenService {

  children: Array<any>
  currentChild: any
  currentSequence: any

  constructor(public http: HttpApiClient) { }

  getChildren(): Observable<Array<Child>> {
    return this.http.get('/listchildren').map(result => this.children = result && result.json());
  }

  getChild(id: number): Observable<Child> {
    return this.http.get('/child/' + id).map(result => result && result.json());
  }

  addChildren(child: Child): Observable<Response> {
    return this.http.post('/registerchild', child);
  }

  editChild(child: Child): Observable<Response> {
    return this.http.post('/editchild/' + child.childId, child);
  }

  deleteChild(id: number) {
    return this.http.delete('/deletechild/' + id);
  }

  getChildSequences(id: number) {
    return this.http.get('/child/' + id + '/sequences')
      .map(res => res.json());
  }

  getSequence(id: number) {
      return this.http.get('/sequences/' + id);
  }

  updatePreferences(childId: number, childPreferences: Preferences): Observable<Response> {
    return this.http.post('/child/' + childId + '/updatepreferences', childPreferences);
  }

}
