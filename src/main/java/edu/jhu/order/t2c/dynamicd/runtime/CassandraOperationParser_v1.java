package edu.jhu.order.t2c.dynamicd.runtime;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

// Cassandra operation is more challenging to extract meta info, so we wrote a simple parser by ourselves to unmarshall
//refer to this commit: https://github.com/OrderLab/cassandra/compare/b697af87f8e1b20d22948390d516dba1fbb9eee7...operation-checker#diff-59b1c007d10de3aae0ccedb7dcd15528f258c44bb59d5e556c0e48b47fc43881
public class CassandraOperationParser_v1 {

    public enum OpType {
        CREATEKEYSPACE("CREATE KEYSPACE"),
        DROPKEYSPACE("DROP KEYSPACE"),
        CREATETABLE("CREATE TABLE"),
        DROPTABLE("DROP TABLE"),
        CREATEINDEX("CREATE INDEX"),
        DROPINDEX("DROP INDEX"),
        CREATETYPE("CREATE TYPE"),
        INSERT("INSERT INTO"),
        UPDATE("UPDATE"),
        DELETE("DELETE"),
        SELECT("SELECT"),
        ASSERT("ASSERT"),
        USE("USE"),

        //intermediate flags to mark a selected region,
        //they should not appear in the final template
        TESTBODYBEGIN("TESTBODYBEGIN"),
        TESTBODYEND("TESTBODYEND"),
        ILLEGAL("ILLEGAL");

        private final String text;

        /**
         * @param text
         */
        OpType(final String text) {
            this.text = text;
        }

        /* (non-Javadoc)
         * @see java.lang.Enum#toString()
         */
        @Override
        public String toString() {
            return text;
        }
    }

    final static String PRIMARYKEY_CHAR = "@PK";
    final static String COMPACTSTORAGE_CHAR = "@CS";

    private static String formatString(String oldStr) {
        String newStr = oldStr;

        //add splits for special characters
        newStr = newStr.replace("(", " ( ");
        newStr = newStr.replace(")", " ) ");
        newStr = newStr.replace(",", " , ");
        newStr = newStr.replace("\u003d", " = ");

        //shrink words
        newStr = newStr.replace("WITH COMPACT STORAGE", " " + COMPACTSTORAGE_CHAR + " ");
        newStr = newStr.replace("PRIMARY KEY", " " + PRIMARYKEY_CHAR + " ");

        return newStr;
    }

    public static Operation createOperation(String queryStr) {
        Symbol.SymbolBuilder builder = new Symbol.SymbolBuilder(CheckerTemplate.symId, CheckerTemplate.cache);
        CassandraOperationParser_v1.OpType opType = CassandraOperationParser_v1.OpType.ILLEGAL;

        //remove end ";"
        String CAPITALIZED_STR = queryStr.toUpperCase().replaceAll("^\\s+", "")
                .replace(";","");
        ;
        for (OpType type : OpType.values()) {
            if (CAPITALIZED_STR.startsWith(type.text)) {
                opType = type;
                break;
            }
        }

        JsonObject tree = new JsonObject();

        String[] splited = formatString(CAPITALIZED_STR.replace(opType.text, "")).split("\\s+");

        if (opType.equals(OpType.CREATETABLE)) {
            int status = 0;
            int index = 0;

            JsonArray array = new JsonArray();
            JsonArray pkarray = new JsonArray();

            JsonObject column = new JsonObject();
            for (String word : splited) {
                if (word.equals(""))
                    continue;

                switch (status) {
                    case 0:
                        tree.addProperty("tablename", builder.getSymbol(word));
                        status++;
                        break;
                    case 1:
                        if (word.equals("(")) {
                            array = new JsonArray();
                        } else if (word.equals(")")) {
                            tree.add("tableinfo", array);
                        } else if (word.equals(PRIMARYKEY_CHAR)) {
                            status++;
                        } else if (word.equals(COMPACTSTORAGE_CHAR)) {
                            break;
                        } else if (!word.equals(",")) {
                            if (index == 0) {
                                column = new JsonObject();
                                column.addProperty("name", builder.getSymbol(word));
                                index = 1;
                            } else {
                                column.addProperty("type", word);
                                array.add(column);
                                index = 0;
                            }
                        }
                        break;
                    case 2:
                        if (word.equals("(")) {
                            pkarray = new JsonArray();
                        } else if (word.equals(")")) {
                            tree.add("primarykeyinfo", pkarray);
                            status--;
                        } else if (!word.equals(",")) {
                            pkarray.add(builder.getSymbol(word));
                        }
                        break;
                }
            }
        } else if (opType.equals(OpType.INSERT)) {
            int status = 0;

            JsonArray array = new JsonArray();
            for (String word : splited) {
                if (word.equals(""))
                    continue;

                switch (status) {
                    case 0:
                        tree.addProperty("tablename", builder.getSymbol(word));
                        status++;
                        break;
                    case 1:
                        if (word.equals("VALUES"))
                            status++;
                        else {
                            if (word.equals("(")) {
                                array = new JsonArray();
                            } else if (word.equals(")")) {
                                tree.add("tableinfo", array);
                            } else if (!word.equals(",")) {
                                array.add(builder.getSymbol(word));
                            }
                        }
                        break;
                    case 2:
                        if (word.equals("(")) {
                            array = new JsonArray();
                        } else if (word.equals(")")) {
                            tree.add("values", array);
                        } else if (!word.equals(",")) {
                            array.add(builder.getSymbol(word));
                        }
                        break;
                }
            }
        } else if (opType.equals(OpType.DELETE)) {
            int status = 0;
            int index = 0;

            JsonArray array = new JsonArray();
            JsonObject object = new JsonObject();
            for (String word : splited) {
                if (word.equals(""))
                    continue;

                switch (status) {
                    case 0:
                        if (!word.equals("FROM")) {
                            tree.addProperty("tablename", builder.getSymbol(word));
                            status++;
                        }
                        break;
                    case 1:
                        if (!word.equals("WHERE")) {
                            if (index == 0) {
                                object = new JsonObject();
                                object.addProperty("left", builder.getSymbol(word));
                                index++;
                            } else if (index == 1) {
                                object.addProperty("relation", word);
                                index++;
                            } else if (index == 2) {
                                object.addProperty("right", builder.getSymbol(word));
                                array.add(object);
                                index = 0;
                            }
                        }
                        break;
                }
            }
            tree.add("constraints", array);

        } else if (opType.equals(OpType.SELECT)) {
            int status = -1;
            int index = 0;

            JsonArray array = new JsonArray();
            JsonObject object = new JsonObject();
            for (String word : splited) {
                if (word.equals(""))
                    continue;

                switch (status) {
                    case -1:
                        if (!word.equals("FROM")) {
                            tree.addProperty("columnname", builder.getSymbol(word));
                            status++;
                        }
                        break;
                    case 0:
                        if (!word.equals("FROM")) {
                            tree.addProperty("tablename", builder.getSymbol(word));
                            status++;
                        }
                        break;
                    case 1:
                        if(word.equals("LIMIT")){
                            status++;
                            break;
                        }
                        if (!word.equals("WHERE")) {
                            if (index == 0) {
                                object = new JsonObject();
                                object.addProperty("left", builder.getSymbol(word));
                                index++;
                            } else if (index == 1) {
                                object.addProperty("relation", word);
                                index++;
                            } else if (index == 2) {
                                object.addProperty("right", builder.getSymbol(word));
                                array.add(object);
                                index = 0;
                                status++;
                            }
                        }
                        break;
                    case 2:
                        tree.addProperty("limit", builder.getSymbol(word));
                        break;
                }
            }
            tree.add("constraints", array);

        }


        int flagIndex = queryStr.indexOf("WITH");
        String flag = (flagIndex == -1) ? "" : queryStr.substring(flagIndex + 5);

        if (opType == CassandraOperationParser_v1.OpType.ILLEGAL) {
            throw new RuntimeException("Cannot parse:" + queryStr);
        }

        Operation op = new Operation(opType.text, flag);
        op.opTree = tree;
        op.symbolMap = builder.getSymbolMap();
        op.queryStr = queryStr;

        //update global id
        //CheckerTemplate.symId = builder.index;
        return op;
    }

}
