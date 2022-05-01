package it.polimi.ingsw.Server.Controller.Network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import it.polimi.ingsw.Server.Controller.Network.Messages.*;

public class  JsonConverter {
    private static GsonBuilder builder = new GsonBuilder();
    private static Gson gson;

    static{
        builder.setPrettyPrinting();
        gson = builder.create();
    }

    public static Message fromJsonToMessage(String jsonString){
        Message message = gson.fromJson(jsonString, Message.class);

        if(message.getMessageType()== TypeOfMessage.Connection){
            ConnectionMessage messageReal=  gson.fromJson(jsonString, ConnectionMessage.class);
            return messageReal;
        }
        if(message.getMessageType()== TypeOfMessage.StudentColour||
                message.getMessageType()== TypeOfMessage.IslandChoice ||
                message.getMessageType()== TypeOfMessage.AssistantCard ||
                message.getMessageType()== TypeOfMessage.CharacterCard ||
                message.getMessageType()== TypeOfMessage.CloudChoice ||
                message.getMessageType()== TypeOfMessage.MoveMotherNature ||
                message.getMessageType()==TypeOfMessage.Where ||
                message.getMessageType()==TypeOfMessage.Number){
            GameMessage messageReal=  gson.fromJson(jsonString, GameMessage.class);
            return messageReal;
        }
        if(message.getMessageType()== TypeOfMessage.Ping){
            PingMessage messageReal=  gson.fromJson(jsonString, PingMessage.class);
            return messageReal;
        }
        if(message.getMessageType()== TypeOfMessage.GameState){
            GameStateMessage messageReal=  gson.fromJson(jsonString, GameStateMessage.class);
            return messageReal;
        }

        return message;
    }


    public static String fromMessageToJson(Message message){
        String jsonString=gson.toJson(message) + "\nEOF\n";
        return(jsonString);
    }

}
