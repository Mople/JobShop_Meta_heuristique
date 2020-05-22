package jobshop.encodings;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Solver;
import jobshop.solvers.DescentSolver;
import jobshop.solvers.GreedySolverEST_SPT;

import java.util.ArrayList;
import java.util.List;

public class TabooSolver  implements Solver {

    private int maxIter;
    private int dureeTaboo;

    public TabooSolver(int maxIter, int dureeTaboo){
        this.dureeTaboo=dureeTaboo;
        this.maxIter=maxIter;
    }
    /** A block represents a subsequence of the critical path such that all tasks in it execute on the same machine.
     * This class identifies a block in a ResourceOrder representation.
     *
     * Consider the solution in ResourceOrder representation
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (0,2) (2,1) (1,1)
     * machine 2 : ...
     *
     * The block with : machine = 1, firstTask= 0 and lastTask = 1
     * Represent the task sequence : [(0,2) (2,1)]
     *
     * */
    static class Block {
        /** machine on which the block is identified */
        final int machine;
        /** index of the first task of the block */
        final int firstTask;
        /** index of the last task of the block */
        final int lastTask;

        Block(int machine, int firstTask, int lastTask) {
            this.machine = machine;
            this.firstTask = firstTask;
            this.lastTask = lastTask;
        }
    }

    /**
     * Represents a swap of two tasks on the same machine in a ResourceOrder encoding.
     *
     * Consider the solution in ResourceOrder representation
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (0,2) (2,1) (1,1)
     * machine 2 : ...
     *
     * The swam with : machine = 1, t1= 0 and t2 = 1
     * Represent inversion of the two tasks : (0,2) and (2,1)
     * Applying this swap on the above resource order should result in the following one :
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (2,1) (0,2) (1,1)
     * machine 2 : ...
     */
    static class Swap {
        // machine on which to perform the swap
        final int machine;
        // index of one task to be swapped
        final int t1;
        // index of the other task to be swapped
        final int t2;

        Swap(int machine, int t1, int t2) {
            this.machine = machine;
            this.t1 = t1;
            this.t2 = t2;
        }

        /** Apply this swap on the given resource order, transforming it into a new solution. */
        public void applyOn(ResourceOrder order) {
            Task taskToSwap = order.tasksByMachine[machine][t1];
            order.tasksByMachine[machine][t1] = order.tasksByMachine[machine][t2];
            order.tasksByMachine[machine][t2] = taskToSwap;
        }
    }

    @Override
    public Result solve(Instance instance, long deadline) {
        //Initialisation
        Result sInit = new GreedySolverEST_SPT().solve(instance,deadline);
        ResourceOrder sStar = new ResourceOrder(sInit.schedule);
        ResourceOrder s = new ResourceOrder(sInit.schedule);
        List<List<Task>> sTaboo = new ArrayList<>();
        sTaboo.add(s.toSchedule().criticalPath());
        int k =0;
        boolean noNeighbors=false;
        while (k<maxIter && !noNeighbors){
            k++;

            //Find neighbors
            List<Swap> currentNeightbors = new ArrayList<>();
            for (Block currentBlock : blocksOfCriticalPath(s)){
                currentNeightbors.addAll(neighbors(currentBlock));
            }

            //Find best neighbour
            int bestScore = Integer.MAX_VALUE;
            ResourceOrder sPrime = null;
            for (Swap currentSwap : currentNeightbors){
                ResourceOrder possibleState = s.copy();
                currentSwap.applyOn(possibleState);
                int score = possibleState.toSchedule().makespan();
                if (score < bestScore && !sTaboo.contains(possibleState.toSchedule().criticalPath())){
                    sPrime = possibleState;
                    bestScore = score;
                }
            }

            //Add this neighbour to Taboo
            if (sPrime!=null) {
                if (sTaboo.size() == dureeTaboo) {
                    sTaboo.remove(sTaboo.get(0));
                }
                sTaboo.add(sPrime.toSchedule().criticalPath());
                s = sPrime;
            }
            else{
                noNeighbors=true;
            }


            //Compare with best solution
            if (bestScore < sStar.toSchedule().makespan()){

                sStar = sPrime;
            }
        }

        return new Result(instance,sStar.toSchedule(),Result.ExitCause.Blocked);


    }

    /** Returns a list of all blocks of the critical path. */
    List<Block> blocksOfCriticalPath(ResourceOrder order) {
        List<Task> criticalPath = order.toSchedule().criticalPath();
        List<Block> result = new ArrayList<>();

        int machine = -1;
        int indexStart = 0;
        int indexEnd = 0;

        for (Task currentTask : criticalPath){
            int currentIndex = criticalPath.indexOf(currentTask);
            //Add the task to the current block if on the same machine
            if (order.instance.machine(currentTask)==machine){
                indexEnd = currentIndex;
            }
            else {
                //If the block is composed of 2+ tasks, add it to the result
                int length = indexEnd - indexStart;
                if (length > 0){
                    int indexMachineStart=0;
                    int indexMachineEnd=0;
                    for (int i=0 ; i<order.instance.numJobs ; i++){
                        if (order.tasksByMachine[machine][i].equals(criticalPath.get(indexStart))){
                            indexMachineStart = i;
                        }
                        else if (order.tasksByMachine[machine][i].equals(criticalPath.get(indexEnd))){
                            indexMachineEnd = i;
                        }
                    }
                    result.add(new Block(machine,indexMachineStart,indexMachineEnd));
                }
                //Initiate new block
                machine = order.instance.machine(currentTask);
                indexStart = currentIndex;
                indexEnd = currentIndex;
            }
        }
        //If the last block is composed of 2+ tasks, add it to the result
        int length = indexEnd - indexStart;
        if (length > 0){
            int indexMachineStart=0;
            int indexMachineEnd=0;
            for (int i=0 ; i<order.instance.numJobs ; i++){
                if (order.tasksByMachine[machine][i].equals(criticalPath.get(indexStart))){
                    indexMachineStart = i;
                }
                else if (order.tasksByMachine[machine][i].equals(criticalPath.get(indexEnd))){
                    indexMachineEnd = i;
                }
            }
            result.add(new Block(machine,indexMachineStart,indexMachineEnd));
        }
        return  result;
    }

    /** For a given block, return the possible swaps for the Nowicki and Smutnicki neighborhood */
    List<Swap> neighbors(Block block) {
        List<Swap> result = new ArrayList<>();
        result.add(new Swap(block.machine,block.firstTask,block.firstTask+1));
        if ((block.lastTask - block.firstTask)>1){
            result.add(new Swap(block.machine,block.lastTask - 1,block.lastTask));
        }
        return result;
    }
}
