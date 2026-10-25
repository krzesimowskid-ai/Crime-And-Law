package pl.crimeandlaw.network;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import pl.crimeandlaw.gang.*;
import static org.junit.jupiter.api.Assertions.*;

class ActionFieldsTest {
    private JsonObject nested(String key){
        JsonElement value=new JsonPrimitive("true");
        for(int i=0;i<10000;i++){var array=new JsonArray();array.add(value);value=array;}
        var object=new JsonObject();object.add(key,value);return object;
    }
    @Test void nestedCollectionsCannotBeUnwrappedAsScalarFields(){
        assertThrows(IllegalArgumentException.class,()->ActionFields.text(nested("action"),"action",""));
        assertThrows(IllegalArgumentException.class,()->ActionFields.flag(nested("shared"),"shared",false));
        assertThrows(IllegalArgumentException.class,()->ActionFields.integer(nested("page"),"page",0));
        assertThrows(IllegalArgumentException.class,()->FinanceView.Query.DEFAULT.update(nested("search")));
        assertThrows(IllegalArgumentException.class,()->BlacklistView.Query.DEFAULT.update(nested("history")));
    }
    @Test void integerFieldsRejectFractionsOverflowAndStringCoercion(){
        for(String value:new String[]{"1.5","2147483648","\"2\"","true","null","[]","{}"}){
            JsonObject object=JsonParser.parseString("{\"page\":"+value+"}").getAsJsonObject();
            assertThrows(IllegalArgumentException.class,()->ActionFields.integer(object,"page",0),value);
        }
        var object=JsonParser.parseString("{\"page\":2147483647}").getAsJsonObject();
        assertEquals(Integer.MAX_VALUE,ActionFields.integer(object,"page",0));
        assertEquals(7,ActionFields.integer(new JsonObject(),"page",7));
    }
    @Test void flagsMustBeBooleansAndTextMustBeText(){
        for(String value:new String[]{"\"true\"","1","null","[]","{}"}){
            var object=JsonParser.parseString("{\"flag\":"+value+"}").getAsJsonObject();
            assertThrows(IllegalArgumentException.class,()->ActionFields.flag(object,"flag",false),value);
        }
        var object=JsonParser.parseString("{\"flag\":true,\"name\":\"Żółty gang\"}").getAsJsonObject();
        assertTrue(ActionFields.flag(object,"flag",false));assertEquals("Żółty gang",ActionFields.text(object,"name",""));
        assertThrows(IllegalArgumentException.class,()->ActionFields.text(object,"flag",""));
    }
}
