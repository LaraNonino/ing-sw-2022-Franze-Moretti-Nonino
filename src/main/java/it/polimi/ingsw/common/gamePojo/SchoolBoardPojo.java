package it.polimi.ingsw.common.gamePojo;

import it.polimi.ingsw.server.model.SchoolBoard;

public class SchoolBoardPojo {

    private PawnsMapPojo professors;
    private PawnsMapPojo diningRoom;
    private PawnsMapPojo entrance;
    private int spareTowers;

    public PawnsMapPojo getProfessors() {
        return professors;
    }

    public void setProfessors(PawnsMapPojo professors) {
        this.professors = professors;
    }

    public PawnsMapPojo getDiningRoom() {
        return diningRoom;
    }

    public void setDiningRoom(PawnsMapPojo diningRoom) {
        this.diningRoom = diningRoom;
    }

    public PawnsMapPojo getEntrance() {
        return entrance;
    }

    public void setEntrance(PawnsMapPojo entrance) {
        this.entrance = entrance;
    }

    public int getSpareTowers() {
        return spareTowers;
    }

    public void setSpareTowers(int spareTowers) {
        this.spareTowers = spareTowers;
    }

    public SchoolBoard getSchoolBoard(){
        SchoolBoard schoolBoard = new SchoolBoard();
        schoolBoard.setSpareTowers(spareTowers);
        schoolBoard.setProfessors(professors.getPawnsMap());
        schoolBoard.setDiningRoom(diningRoom.getPawnsMap());
        schoolBoard.setEntrance(entrance.getPawnsMap());
        return schoolBoard;
    }
}
