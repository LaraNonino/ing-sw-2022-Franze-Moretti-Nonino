package it.polimi.ingsw.Server.Controller;

import it.polimi.ingsw.Server.Controller.Network.Lobby;
import it.polimi.ingsw.Server.Controller.Network.LobbyManager;
import it.polimi.ingsw.Server.Controller.Network.StopManager;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerController {

    ///////////////////////////////////////// THREAD POOL //////////////////////////////////
    private static ServerController instance;
    private ExecutorService executorService;

    private Map<Integer, GameController> currentGames;
    private Lobby toStart;
    private Integer toStop;

    private ServerController(){
        this.instance=null;
        this.currentGames=new HashMap<>();
        executorService= Executors.newCachedThreadPool();
    }

    public static ServerController getInstance(){
        if (instance == null){
            instance = new ServerController();
        }
        return instance;
    }



    /* ATTENZIONE:
    *  executorService non va bene perchè qualunque cosa facciamo per cancellare un gameController ad esempio
    * aggiorniamo un attributo o altro essendo 2 thread separati è possibile che ancora il metodo di gameController
    * sia in esecuzione -> problema di concorrenza, serve qualche altra collezione di Thread che permette di fare
    *  join() per aspettare la terminazione e poi cancellare
     */

    public void play(){
        // thread that starts games
        LobbyManager lobbyManager = LobbyManager.getInstance();
        Thread t1 = new Thread(lobbyManager);
        t1.start();

        //thread that stop games
        StopManager stopManager = StopManager.getInstance();
        Thread t2 = new Thread(stopManager);
        t2.start();
    }


    public void start(Lobby toStart){
        this.toStart = toStart;
        boolean check=(this.toStart==null);
        System.out.println(check);

        System.out.println("creato game controller per la lobby");
        GameController gameController = new GameController(toStart, toStart.getGameMode().isExpert());
        currentGames.put(gameController.getGameID(), gameController);
        executorService.submit(gameController);

    }

    public void setToStop(Integer toStop){
            this.toStop = toStop;
            removeCurrentGame(toStop);
    }

    public void removeCurrentGame(int gameID){
        this.currentGames.remove(gameID);
    }

    public Map<Integer, GameController> getCurrentGames() {
        return currentGames;
    }


}
