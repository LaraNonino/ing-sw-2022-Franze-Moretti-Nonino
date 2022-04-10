package it.polimi.ingsw.Model.Messages;

public class MoveMotherNatureMessage extends ClientMessage {
    private Integer numOfHops;
    private GameState gameState;

    public Integer getNumOfHops() {
        return numOfHops;
    }

    public GameState getGameState() {
        return gameState;
    }
}
