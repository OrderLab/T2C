package edu.jhu.order.t2c.dynamicd.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class helps to *mutate* a template, which essentially generalize it by loosing the constraints on operation traces.
 */
public class TemplateMutator {

    //mutation rules
    public enum MutationType {
        REDUCE,
        INSERT,
        DUPLICATE,
        REORDER;
    }

    public static List<CheckerTemplate> mutate(CheckerTemplate template, int depth)
    {
        List<CheckerTemplate> mutatedList = new ArrayList<>();
        //save original
        mutatedList.add(template);
        for (MutationType mtype : MutationType.values()) {
            if(mtype.equals(MutationType.DUPLICATE))
            {
                for(int i=0;i< template.operations.size();++i)
                {
                    CheckerTemplate template1 = template.clone();
                    template1.operations.add(i+1, template1.operations.get(i));
                    template1.templateSource += "_mutated_dup_"+i;
                    mutatedList.add(template1);
                }
            }
            else if(mtype.equals(MutationType.REDUCE))
            {
                for(int i=0;i< template.operations.size();++i)
                {
                    CheckerTemplate template1 = template.clone();
                    template1.operations.remove(i);
                    template1.templateSource += "_mutated_reduce_"+i;
                    mutatedList.add(template1);
                }
            }
            else if(mtype.equals(MutationType.INSERT))
            {
                for(int i=0;i< template.operations.size();++i)
                {
                    CheckerTemplate template1 = template.clone();
                    template1.operations.add(i+1, new Operation.WildcardOp());
                    template1.templateSource += "_mutated_insert_"+i;
                    mutatedList.add(template1);
                }
            }
            else if(mtype.equals(MutationType.REORDER))
            {
                for(int i=0;i< template.operations.size()-1;++i)
                {
                    CheckerTemplate template1 = template.clone();
                    Collections.swap(template1.operations, i,i+1);
                    template1.templateSource += "_mutated_reorder_"+i;
                    mutatedList.add(template1);
                }
            }
        }

        return mutatedList;
    }




}
