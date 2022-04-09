package it.polimi.ingsw.Controller;

import it.polimi.ingsw.Controller.ActionPhase;
import it.polimi.ingsw.Controller.GameController;
import it.polimi.ingsw.Exception.EndGameException;
import it.polimi.ingsw.Model.Player;

public class ActionPhaseLastRound extends ActionPhase {
    public ActionPhaseLastRound(GameController gc) {
        super(gc);
    }

    @Override
    public void handle() {
        MessageHandler messageHandler = super.gameController.getMessageHandler();

        try {
            for (Player p : turnOrder) {
                super.moveStudents();
                super.moveMotherNature(p);
            }
        } catch (EndGameException exception){
            exception.printStackTrace();
        }

        this.gameController.setGameOver(true);
    }
}
