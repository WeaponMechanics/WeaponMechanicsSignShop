package me.cjcrafter.weaponmechanicssignshop;

import me.deecaad.core.file.Configuration;
import me.deecaad.core.file.IValidator;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;

public class SignMessageValidator implements IValidator {

    @Override
    public String getKeyword() {
        return "No_Permission_Message";
    }

    @Override
    public void validate(Configuration configuration, SerializeData ignore) throws SerializerException {
        SerializeData data = new SerializeData("Sign Messages", ignore.file, null, ignore.config);

        configuration.set("Identifier", data.of("Identifier").assertExists().get());
        configuration.set("No_Permission_Message", data.of("No_Permission_Message").assertExists().getAdventure());
        configuration.set("Buy_Message", data.of("Buy_Message").assertExists().getAdventure());
        configuration.set("Not_Enough_Money_Message", data.of("Not_Enough_Money_Message").assertExists().getAdventure());
    }
}
