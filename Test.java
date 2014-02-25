package p2pfilesharing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public class Test {

    public static void main(String[] args) throws IOException {


        Map<String, Object> parameters = new HashMap<String, Object>();
        Map<String, Object> copy = new HashMap<String, Object>();
        Map<String, Object> temp = new HashMap<String, Object>();

        Set<String> p1 = new HashSet<String>();
        p1.add("p1");
        Set<String> p2 = new HashSet<String>();
        p2.add("p1");
        p2.add("p2");
        Set<String> p3 = new HashSet<String>();
        p3.add("p2");
        p3.add("p3");
        Set<String> p4 = new HashSet<String>();
        p4.add("p3");
        p4.add("p4");

        parameters.put("f1", p1);
        parameters.put("f2", p2);
        parameters.put("f3", p3);

        copy.put("f1", p1);
        copy.put("f2", p2);
        copy.put("f3", p4);

	temp.put("f3", p4);

        MapOperator.printMap(copy);
        MapOperator.mergeTwoMaps(parameters, copy);
        MapOperator.printMap(copy);
	MapOperator.queryMap(parameters,temp);
	MapOperator.printMap(temp);
        MapOperator.deleteMap(parameters, copy);
        MapOperator.printMap(copy);
    }
}
