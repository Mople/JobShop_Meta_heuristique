package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Solver;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;

import java.util.ArrayList;
import java.util.List;

public class DescentSolver implements Solver {

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
        Result result = new GreedySolverEST_SPT().solve(instance,deadline);
        ResourceOrder state = new ResourceOrder(result.schedule);

        boolean improve = true;
        while (improve){
            //Find neighbors
            List<Swap> currentNeightbors = new ArrayList<>();
            for (Block currentBlock : blocksOfCriticalPath(state)){
                currentNeightbors.addAll(neighbors(currentBlock));
            }

            //Find best neighbour
            int bestScore = result.schedule.makespan();
            ResourceOrder bestNextState = null;
            for (Swap currentSwap : currentNeightbors){
                ResourceOrder possibleState = state.copy();
                currentSwap.applyOn(possibleState);
                int score = possibleState.toSchedule().makespan();
                if (score < bestScore){
                    bestNextState = possibleState;
                    bestScore = score;
                }
            }

            //Compare with current state
            if (bestScore < state.toSchedule().makespan()){

                state = bestNextState;
            }
            else {
                improve = false;
            }
        }
        return new Result(instance,state.toSchedule(),Result.ExitCause.Blocked);
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
