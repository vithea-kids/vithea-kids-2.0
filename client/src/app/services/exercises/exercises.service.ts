import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { Response } from '@angular/http';
import { HttpApiClient } from '../http/http-api-client.service';
import { Exercise } from '../../models/exercise'


@Injectable()
export class ExercisesService {

  exercises: Array<any>

  constructor (public http: HttpApiClient) {}

  getExercises(): Observable<Array<Exercise>> {
    return this.http.get('/listexercises').map(result => this.exercises = result && result.json());
  }

  getExercise(id: number): Observable<Exercise> {
    return this.http.get('/exercise/' + id).map(result => result && result.json());
  }

  addExercise(exercise: Exercise): Observable<Response> {
    return this.http.post('/registerexercise', exercise);
  }

  editExercise(exercise: Exercise): Observable<Response> {
    return this.http.post('/editexercise/' + exercise.exerciseId, exercise);
  }

  deleteExercise(id: number) {
    return this.http.delete('/deleteexercise/' + id);
  }

}
