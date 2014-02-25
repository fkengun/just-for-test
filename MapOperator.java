/*
 * This map operator class is a helper, usef for a series of standard
 * operations on the hashmap.
 * 
 */
package p2pfilesharing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public class MapOperator {

    /*
     * input two maps and the merge result return in map2
     */
    public static void mergeTwoMaps(Map<String, Object> map1, Map<String, Object> map2) {

        Set<String> map1KeySet = map1.keySet();
        Set<String> map2KeySet = map2.keySet();
        for (String key : map1KeySet) {
            if (map2KeySet.contains(key)) {
                Set<String> map2Value = new HashSet<String>();
                for (String value : (Set<String>) map2.get(key)) {
                    map2Value.add(value);
                }
                for (String value : (ArrayList<String>) map1.get(key)) {
                    map2Value.add(value);
                }
                map2.put(key, map2Value);
            } else {
                Set<String> values = new HashSet<String>();
                for (String value : (ArrayList<String>) map1.get(key)) {
                    values.add(value);
                }
                map2.put(key, values);
            }
        }
    }

    /*
     * the query database is map1
     * input two maps and the query result is returned in map2
     */
    public static void queryMap(Map<String, Object> map1, Map<String, Object> map2) {

        Set<String> map1KeySet = map1.keySet();
        Set<String> map2KeySet = map2.keySet();
        for (String key : map1KeySet) {
            if (map2KeySet.contains(key)) {
                Set<String> map2Value = new HashSet<String>();
                for (String value : (Set<String>) map1.get(key)) {
                    map2Value.add(value);
                }
                map2.put(key, map2Value);
            }
        }
    }

    /*
     * input two maps, delete map1 from map2, (keys)
     * map2 is the output
     */
    public static void deleteMap(Map<String, Object> map1, Map<String, Object> map2) {

        Set<String> map1KeySet = map1.keySet();
        Set<String> map2KeySet = map2.keySet();
        for (String key : map1KeySet) {
            if (map2KeySet.contains(key)) {
                Set<String> map2Value = new HashSet<String>();
                for (String value : (Set<String>) map2.get(key)) {
                    map2Value.add(value);
                }
                for (String value : (ArrayList<String>) map1.get(key)) {
                    map2Value.remove(value);
                }
                map2.put(key, map2Value);

            }
        }
        for (String key : map2.keySet()) {
            if (map2.get(key) == null) {
                map2.remove(key);
            }
        }
    }

    /*
     * print the map for checking
     */
    public static void printMap(Map<String, Object> map) {

        Set<String> mapKeySet = map.keySet();
        for (String key : mapKeySet) {
            System.out.print(key + ": ");
            for (String value : (Set<String>) map.get(key)) {
                System.out.print(value + " ");
            }
            System.out.println();
        }
        System.out.println();
    }
}
