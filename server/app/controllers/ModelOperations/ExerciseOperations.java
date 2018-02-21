/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package controllers.ModelOperations;

import models.Caregiver;
import models.Exercise;
import models.MultipleChoice;
import play.data.DynamicForm;

/**
 *
 * @author silvi
 */
public interface ExerciseOperations {
    
    
    public Exercise createExercise(DynamicForm form);
    public void deleteExercise(long exerciseId, Caregiver loggedCaregiver, MultipleChoice exercise);
    public Exercise editExercise(DynamicForm registerExerciseForm, long id, Caregiver loggedCaregiver);
    
}