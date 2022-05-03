package it.polimi.ingsw.server.controller.logic;

import it.polimi.ingsw.common.gamePojo.*;
import it.polimi.ingsw.server.controller.characters.CharacterEffect;
import it.polimi.ingsw.server.controller.network.Lobby;
import it.polimi.ingsw.server.controller.network.MessageHandler;
import it.polimi.ingsw.common.messages.UpdateMessage;
import it.polimi.ingsw.server.controller.network.PlayerManager;
import it.polimi.ingsw.server.model.*;
import it.polimi.ingsw.server.model.Character;
import it.polimi.ingsw.server.model.Cloud;

import java.net.Socket;
import java.net.SocketException;

import java.util.*;

public class GameController implements Runnable  {
    private static int gameID = 1;

    private SetUpPhase setUpPhase;
    private PianificationPhase pianificationPhase;
    private ActionPhase actionPhase;

    private GamePhase currentPhase;

    private boolean gameOver=false;

    private boolean isLastRoundFinishedAssistantCards = false;
    private boolean isLastRoundFinishedStudentsBag = false;
    private boolean isFinishedTowers = false;
    private boolean isThreeOrLessIslands = false;

    private Game game;
    private boolean expert;
    private MessageHandler messageHandler;
    private Player currentPlayer;
    private Player winner;
    private Lobby lobby;

    private PianificationResult pianificationResult;
    private ActionResult actionResult = null;

    private Map<Character, CharacterEffect> characterEffects; // per gli effetti

    public GameController(Lobby lobby, boolean expert){
        this.game=new Game(lobby.getUsersNicknames(), this.gameID);
        this.expert=expert;
        this.gameID ++;
        this.characterEffects = new HashMap<>();
        this.lobby=lobby;

        this.messageHandler= new MessageHandler(lobby);
        this.setTimeout();
    }

    public void run(){

        this.setUpPhase=new SetUpPhase(this);
        this.pianificationPhase=new PianificationPhase(this);
        this.actionPhase=new ActionPhase(this);

        currentPhase = setUpPhase;
        SetUpResult setUpResult = setUpPhase.handle();


        currentPlayer = setUpResult.getFirstRandomPianificationPlayer();


        do{
            currentPhase = pianificationPhase;
            if (actionResult!=null) {
                currentPlayer = actionResult.getFirstPianificationPlayer();
            }

            //System.out.println("\n--------------------------------------PIANIFICATION PHASE----------------------------------------\n");

            pianificationResult = this.pianificationPhase.handle(currentPlayer);

            isLastRoundFinishedAssistantCards = pianificationResult.isFinishedAssistantCard();
            isLastRoundFinishedStudentsBag = pianificationResult.isFinishedStudentBag();

            currentPhase = actionPhase;
            HashMap<Player, Integer> maximumMovements = pianificationResult.getMaximumMovements();
            List<Player> turnOrder = pianificationResult.getTurnOrder();

            //System.out.println("\n--------------------------------------ACTION PHASE----------------------------------------\n");

            actionResult = this.actionPhase.handle(turnOrder, maximumMovements, isLastRoundFinishedStudentsBag);
            isFinishedTowers = actionResult.isFinishedTowers();
            isThreeOrLessIslands = actionResult.isThreeOrLessIslands();

        }
        while(!(isFinishedTowers || isThreeOrLessIslands || isLastRoundFinishedStudentsBag || isLastRoundFinishedAssistantCards));

        //System.out.println("\n--------------------------------------GAME ENDED----------------------------------------\n");

        calculateWinner(); //se winner è null, allora la partita è finita in pareggio

        if (winner != null){
                messageHandler.stringMessageToAllClients("The winner is " + this.winner.toString());
        }
        else{
                messageHandler.stringMessageToAllClients("There is no winner.");
            }



        messageHandler.stringMessageToAllClients( "Students left in Studentbag:" + this.game.getStudentsBag().pawnsNumber());
        for (Player player : this.game.getPlayers()){
            messageHandler.stringMessageToAllClients(player.getNickname()+": " + player.getSchoolBoard().getSpareTowers() + " towers left on schoolboard");
        }

        //System.out.println("Students left in Studentbag:" + this.game.getStudentsBag().pawnsNumber());
        //for(Player p : this.game.getPlayers())
        //    System.out.println(p.getNickname()+": " + p.getSchoolBoard().getSpareTowers() + " towers left on schoolboard");

        ServerController.getInstance().setToStop(this.getGameID());
    }


    /** stops the game if a player do not answer for more than 3 minutes
     *
     */
    public void setTimeout(){
        for(Socket socket: lobby.getUsersReadyToPlay().values()){
            //il turno del giocatore dura 3 minuti al massimo: se non risponde la partita finisce
            try {
                socket.setSoTimeout(180000);
            } catch (SocketException e) {
                e.printStackTrace();
                ServerController.getInstance().setToStop(gameID);
            }
        }
    }

    public synchronized void update(){
        UpdateMessage updateMessage= new UpdateMessage(this.getGameState());


        for(PlayerManager playerManager:messageHandler.getPlayerManagerMap().values()){
            playerManager.sendMessage(updateMessage);
        }

        return;
    }

    public synchronized void updateSinglePlayer(String nickname){
        PlayerManager playerManager= messageHandler.getPlayerManager(nickname);
        UpdateMessage updateMessage= new UpdateMessage(this.getGameState());
        playerManager.sendMessage(updateMessage);

        return;
    }



    /**sets the GameController.winner
     * if it remains null, it means ther is no winner*/
    public void calculateWinner(){

       /*devo controllare nello stesso ordine in cui si arresta il gioco:
       1. un giocatore finisce le torri
       2. quante torri sono state piazzate per ciascun giocatore */

        //1. controllo se un giocatore ha piazzato tutte le torri

        for(Player p : getGame().getPlayers()){
            if (p.getSchoolBoard().getSpareTowers() <= 0){
                winner = p;
                return;
            }
        }

        //calcolo il vincitore contando le torri piazzate

        //map of the players and the number of tower that they have placed
        Map<Player, Integer> towerPlaced = new HashMap<Player, Integer>();
        for(Player p : getGame().getPlayers()){
            towerPlaced.put(p, 0);
        }

        for(Island i : getGame().getIslands()){
            if(i.getOwner(getGame()) != null)
                towerPlaced.put(i.getOwner(getGame()), towerPlaced.get(i.getOwner(getGame())) + i.getTowerCount());
        }

        int maxPlaced = 0;
        Player tempWinner = null;
        for(Player p : getGame().getPlayers()){
            if (towerPlaced.get(p) > maxPlaced){
                maxPlaced = towerPlaced.get(p);
                tempWinner = p;
            }
        }

        //verifico che non ci siano altri giocatori con lo stesso numero di torri piazzate
        boolean isOnly = true;
        for(Player p : getGame().getPlayers()){
            if (towerPlaced.get(p) == maxPlaced && !p.equals(tempWinner)){
                isOnly = false;
            }
        }
        if (isOnly == true){
            winner = tempWinner;
            return;
        }

        //nel caso ci sia parità di torri piazzate conto il numero dei professori

        int maxProf = 0;
        tempWinner = null;
        for(Player p : getGame().getPlayers()){
            if (p.getSchoolBoard().getProfessors().pawnsNumber() > maxProf){
                maxProf = p.getSchoolBoard().getProfessors().pawnsNumber();
                tempWinner = p;
            }
        }

        //verifico che non ci siano altri giocatori con lo stesso numero di professori
        isOnly = true;
        for(Player p : getGame().getPlayers()){
            if (p.getSchoolBoard().getProfessors().pawnsNumber() == maxProf && !p.equals(tempWinner)){
                isOnly = false;
            }
        }
        if (isOnly == true){
            winner = tempWinner;
            return;
        }

    }

    public Game getGame() {
        return game;
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }

    public MessageHandler getMessageHandler() {
        return messageHandler;
    }

    public boolean isExpert() {
        return expert;
    }

    public void setCurrentPlayer(Player currentPlayer) {
        this.currentPlayer = currentPlayer;
        //it sets myTurn of all players in PlayerManager
        this.getMessageHandler().setTurn(currentPlayer.getNickname());
    }

    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    public Integer getGameID() {
        return gameID;
    }

    public Map<Character, CharacterEffect> getCharacterEffects() {
        return characterEffects;
    }

    /** used by Char4 doEffect()
     * it adds to the maximum movements of mother nature permitted by the assistant card chosen by the player
     * two additional steps
     * @param player
     */
    public void addTwoMovements(Player player){
        Integer numMax= pianificationResult.getMaximumMovements().get(player)+2;
        HashMap<Player,Integer> newMaxMovements= pianificationResult.getMaximumMovements();
        newMaxMovements.put(player,numMax);
        pianificationResult.setMaximumMovements(newMaxMovements);
    }

    public ActionPhase getActionPhase() {
        return actionPhase;
    }

    public GamePhase getCurrentPhase() {
        return currentPhase;
    }

    public GameStatePojo getGameState(){
        GameStatePojo gameStatePojo = new GameStatePojo();
        gameStatePojo.setCurrentPhase(this.currentPhase instanceof ActionPhase ? Phase.ACTION : Phase.PIANIFICATION);
        gameStatePojo.setGameId(this.getGameID());
        gameStatePojo.setWinner(this.winner.getNickname());
        gameStatePojo.setGameOver(this.gameOver);
        gameStatePojo.setExpert(this.expert);

        PlayerPojo pojoCurrentPlayerPojo = currentPlayer.toPojo();
        gameStatePojo.setCurrentPlayer(pojoCurrentPlayerPojo);

        List<PlayerPojo> pojoPlayerPojos = new ArrayList<>();
        for (Player p: this.game.getPlayers()){
            PlayerPojo pojoPlayerPojo = p.toPojo();
            pojoPlayerPojos.add(pojoPlayerPojo);
        }
        gameStatePojo.setPlayers(pojoPlayerPojos);

        PawnsMapPojo pojoProfessorsLeft = new PawnsMapPojo(this.game.getProfessorsLeft());
        gameStatePojo.setProfessorsLeft(pojoProfessorsLeft);

        PawnsMapPojo pojoStudentsBag = new PawnsMapPojo(this.game.getStudentsBag());
        gameStatePojo.setStudentsBag(pojoStudentsBag);

        gameStatePojo.setCoinSupply(this.game.getCoinSupply());

        List<CharacterPojo> pojoCharacterPojos = new ArrayList<>();
        for (Character c : this.game.getCharacters()){
            CharacterPojo pojoCharacterPojo = c.toPojo();
            pojoCharacterPojos.add(pojoCharacterPojo);
        }
        gameStatePojo.setCharacters(pojoCharacterPojos);

        CharacterPojo pojoActiveCharacterPojo = this.game.getActiveEffect().toPojo();
        gameStatePojo.setActiveEffect(pojoActiveCharacterPojo);

        List<IslandPojo> pojoIslandPojos = new ArrayList<>();
        for(Island i: game.getIslands()){
            IslandPojo pojoIslandPojo = i.toPojo();
            pojoIslandPojos.add(pojoIslandPojo);
        }
        gameStatePojo.setIslands(pojoIslandPojos);

        List<it.polimi.ingsw.common.gamePojo.Cloud> pojoClouds = new ArrayList<>();
        for(Cloud c: game.getClouds()){
            it.polimi.ingsw.common.gamePojo.Cloud pojoCloud = c.toPojo();
            pojoClouds.add(pojoCloud);
        }
        gameStatePojo.setClouds(pojoClouds);

        return gameStatePojo;
    }

}
