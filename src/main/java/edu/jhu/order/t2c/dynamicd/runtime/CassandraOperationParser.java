package edu.jhu.order.t2c.dynamicd.runtime;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

// Cassandra operation is more challenging to extract meta info, so we wrote a simple parser by ourselves to unmarshall
//refer to this commit: https://github.com/OrderLab/cassandra/compare/b697af87f8e1b20d22948390d516dba1fbb9eee7...operation-checker#diff-59b1c007d10de3aae0ccedb7dcd15528f258c44bb59d5e556c0e48b47fc43881
public class CassandraOperationParser {

    public enum OpType {
        CREATEKEYSPACE("CREATE KEYSPACE"),
        CREATEUSER("CREATE USER"),
        CREATEINDEX("CREATE INDEX"),
        CREATETABLE("CREATE TABLE"),
        CREATEROLE("CREATE ROLE"),
        CREATECUSTOMINDEX("CREATE CUSTOM INDEX"),
        CREATETYPE("CREATE TYPE"),
        CREATEFUNCTION("CREATE FUNCTION"),
        CREATEORREPLACEFUNCTION("CREATE OR REPLACE FUNCTION"),
        CREATEAGGREGATE("CREATE AGGREGATE"),
        
        REVOKEALTER("REVOKE ALTER"),
        REVOKESELECT("REVOKE SELECT"),
        
        ALTERKEYSPACE("ALTER KEYSPACE"),
        ALTERROLE("ALTER ROLE"),
        ALTERUSER("ALTER USER"),
        ALTERTABLE("ALTER TABLE"),
        
        GRANTSELECT("GRANT SELECT"),
        GRANTALTERONROLE("GRANT ALTER ON ROLE"),
        
        LISTALLPERMISSIONS("LIST ALL PERMISSIONS"),
        LISTROLESOF("LIST ROLES OF"),

        DROPTABLE("DROP TABLE"),
        DROPROLE("DROP ROLE"),
        DROPUSER("DROP USER"),
        DROPKEYSPACE("DROP KEYSPACE"),
        DROPINDEX("DROP INDEX"),
        DROPTYPE("DROP TYPE"),
        DROPFUNCTION("DROP FUNCTION"),
        DROPAGGREGATE("DROP AGGREGATE"),
        
        BEGINBATCH("BEGIN BATCH"),
        INSERT("INSERT INTO"),
        UPDATE("UPDATE"),
        DELETE("DELETE"),
        SELECT("SELECT"),
        TRUNCATE("TRUNCATE"),
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
        //System.out.println("#### cassandra operation "+queryStr);
        Symbol.SymbolBuilder builder = new Symbol.SymbolBuilder(CheckerTemplate.symId, CheckerTemplate.cache);
        CassandraOperationParser.OpType opType = CassandraOperationParser.OpType.ILLEGAL;

        boolean ifQueryOp = false;

        //remove end ";"
        String CAPITALIZED_STR = queryStr.toUpperCase().replaceAll("^\\s+", "")
                .replace(";","");
        for (OpType type : OpType.values()) {
            if (CAPITALIZED_STR.startsWith(type.text)) {
                opType = type;
                if(opType.text.equals("SELECT"))
                    ifQueryOp=true;

                break;
            }
        }

        JsonObject tree = new JsonObject();

        String[] splited = formatString(CAPITALIZED_STR.replace(opType.text, "")).split("\\s+");

        int count =0 ;
        for(String s: splited)
        {
            tree.addProperty(String.valueOf(count), builder.getSymbol(s));
            count ++;
        }

        if (opType == CassandraOperationParser.OpType.ILLEGAL) {
            throw new RuntimeException("Cannot parse:" + queryStr);
        }

        Operation op = new Operation(opType.text,"");
        op.opTree = tree;
        op.symbolMap = builder.getSymbolMap();
        op.ifQueryOp = ifQueryOp;
        op.queryStr = queryStr;

        //update global id
        //CheckerTemplate.symId = builder.index;
        return op;
    }

}

