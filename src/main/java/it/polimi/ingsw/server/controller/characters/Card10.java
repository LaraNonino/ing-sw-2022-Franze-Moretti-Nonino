package it.polimi.ingsw.server.controller.characters;

import it.polimi.ingsw.common.gamePojo.ColourPawn;
import it.polimi.ingsw.common.messages.Message;
import it.polimi.ingsw.server.controller.logic.GameController;
import it.polimi.ingsw.server.controller.network.MessageHandler;
import it.polimi.ingsw.common.messages.GameMessage;
import it.polimi.ingsw.common.messages.TypeOfMessage;
import it.polimi.ingsw.server.controller.network.PlayerManager;

public class Card10 extends CharacterEffect{
    private GameController gameController;

    public Card10(GameController gameController){
        this.gameController=gameController;
    }

    public void doEffect() {
        boolean valid;
        int num=0;
        int i, colourEntrance, colourDining;
        MessageHandler messageHandler = this.gameController.getMessageHandler();
        String currPlayer= gameController.getCurrentPlayer().getNickname();
        PlayerManager playerManager= messageHandler.getPlayerManager(currPlayer);
        Message errorGameMessage;
        GameMessage gameMessage;

        do{
            valid=true;
            gameMessage = playerManager.readMessage(TypeOfMessage.Number);
            num = gameMessage.getValue();
            if(num<0 || num >2){
                valid = false;
                errorGameMessage=new Message(TypeOfMessage.Error);
                playerManager.sendMessage(errorGameMessage);
            }
        }while(!valid);

        for (i=0; i<num; i++){
            do{
                valid = true;
                // to user: choose one color pawn
                gameMessage = playerManager.readMessage(TypeOfMessage.StudentColour);
                colourEntrance = gameMessage.getValue();
                if(colourEntrance<=-1 || colourEntrance >=5){
                    valid=false;
                    // to user: index not valid
                    errorGameMessage=new Message(TypeOfMessage.Error);
                    playerManager.sendMessage(errorGameMessage);
                }
                if(valid){
                    if (gameController.getCurrentPlayer().getSchoolBoard()
                            .getEntrance().get(ColourPawn.get(colourEntrance)) <= 0){
                        valid = false;
                        //to user: change color pawn to move, you don't have that color
                        errorGameMessage=new Message(TypeOfMessage.Error);
                        playerManager.sendMessage(errorGameMessage);
                    }
                }
                if(valid && gameController.getCurrentPlayer().getSchoolBoard().getDiningRoom().
                        get(ColourPawn.get(colourEntrance))>=10) {
                    valid = false;
                    // to user: your school board in that row of your dining room is full
                }

            }while(!valid);

            do{
                valid=true;
                gameMessage = playerManager.readMessage(TypeOfMessage.StudentColour);
                colourDining= gameMessage.getValue();
                if(gameController.getCurrentPlayer().getSchoolBoard().getDiningRoom().get(ColourPawn.get(colourDining)) <=0){
                    valid = false;
                    errorGameMessage=new Message(TypeOfMessage.Error);
                    playerManager.sendMessage(errorGameMessage);
                }
            }while(!valid);

            gameController.getCurrentPlayer().getSchoolBoard().swap(ColourPawn.get(colourEntrance), ColourPawn.get(colourDining), gameController.getGame());
        }
    }
}