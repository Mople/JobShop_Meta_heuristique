package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Solver;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;

import java.util.ArrayList;

public class GreedySolverEST_SPT implements Solver {

    @Override
    public Result solve(Instance instance, long deadline) {
        int[] jobsTime = new int[instance.numJobs];
        int[] machinesTime = new int[instance.numMachines];

        ResourceOrder sol = new ResourceOrder(instance);

        ArrayList<Task> listTaskRealisable = new ArrayList<>();

        //Initialisation
        for (int i=0; i<instance.numJobs;i++){
            listTaskRealisable.add(new Task(i,0));
        }

        //Loop
        while(!listTaskRealisable.isEmpty()){
            //Choose the task = minimum duration
            ArrayList<Task> newListTask = new ArrayList<>();
            int earlyStartTime = Integer.MAX_VALUE;
            for (Task currentTask : listTaskRealisable){
                int start_time = Integer.max(jobsTime[currentTask.job],machinesTime[instance.machine(currentTask)]);
                if (start_time < earlyStartTime) {
                    earlyStartTime =start_time;
                    newListTask.clear();
                    newListTask.add(currentTask);
                }
                if (start_time==earlyStartTime) {
                    newListTask.add(currentTask);
                }
            }

            int minDuration =Integer.MAX_VALUE;
            Task taskChosen = null;
            for (Task currentTask : newListTask){
                int timeTask = instance.duration(currentTask.job,currentTask.task);
                if(timeTask<minDuration){
                    taskChosen = currentTask;
                    minDuration = timeTask;
                }
            }

            //Put the task on the machine
            int machine = instance.machine(taskChosen);
            sol.tasksByMachine[machine][sol.nextFreeSlot[machine]] = taskChosen;
            sol.nextFreeSlot[machine]++;
            jobsTime[taskChosen.job]+=instance.duration(taskChosen);
            machinesTime[instance.machine(taskChosen)]+=instance.duration(taskChosen);

            //Update task list
            listTaskRealisable.remove(taskChosen);
            if(taskChosen.task<instance.numTasks - 1){
                listTaskRealisable.add(new Task(taskChosen.job,taskChosen.task + 1));
            }
        }
        return new Result(instance,sol.toSchedule(),Result.ExitCause.Blocked);
    }
}
