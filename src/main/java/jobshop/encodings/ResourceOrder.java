package jobshop.encodings;

import jobshop.Encoding;
import jobshop.Instance;
import jobshop.Schedule;

import java.util.Comparator;
import java.util.Optional;
import java.util.stream.IntStream;

public class ResourceOrder extends Encoding {

    // for each machine m, taskByMachine[m] is an array of tasks to be
    // executed on this machine in the same order
    public final Task[][] tasksByMachine;

    // for each machine, indicate on many tasks have been initialized
    public final int[] nextFreeSlot;

    /** Creates a new empty resource order. */
    public ResourceOrder(Instance instance)
    {
        super(instance);

        // matrix of null elements (null is the default value of objects)
        tasksByMachine = new Task[instance.numMachines][instance.numJobs];

        // no task scheduled on any machine (0 is the default value)
        nextFreeSlot = new int[instance.numMachines];
    }

    /** Creates a resource order from a schedule. */
    public ResourceOrder(Schedule schedule)
    {
        super(schedule.pb);
        Instance pb = schedule.pb;

        this.tasksByMachine = new Task[pb.numMachines][];
        this.nextFreeSlot = new int[instance.numMachines];

        for(int m = 0 ; m<schedule.pb.numMachines ; m++) {
            final int machine = m;

            // for thi machine, find all tasks that are executed on it and sort them by their start time
            tasksByMachine[m] =
                    IntStream.range(0, pb.numJobs) // all job numbers
                            .mapToObj(j -> new Task(j, pb.task_with_machine(j, machine))) // all tasks on this machine (one per job)
                            .sorted(Comparator.comparing(t -> schedule.startTime(t.job, t.task))) // sorted by start time
                            .toArray(Task[]::new); // as new array and store in tasksByMachine

            // indicate that all tasks have been initialized for machine m
            nextFreeSlot[m] = instance.numJobs;
        }
    }

    @Override
    public Schedule toSchedule() {
        int[][] startTimes = new int[instance.numJobs][instance.numTasks];

        //Next free time for a machine
        int[] timesMachineFree = new int[instance.numMachines];
        //Number of Task done by a machine
        int[] numberTaskByMachine = new int[instance.numMachines];

        //Next task to schedule by job
        int[] jobNextTask = new int[instance.numJobs];

        for(int i=0;i<instance.numTasks*instance.numJobs;i++) {
            boolean foundExecTask =false;
            int m=0;
            while (!foundExecTask && m<instance.numMachines ){
                if (numberTaskByMachine[m]<instance.numJobs) {
                    //Read the first task not scheduled on a machine
                    Task currentTask = tasksByMachine[m][numberTaskByMachine[m]];

                    //Check if the predecessors are already scheduled
                    if (currentTask.task == jobNextTask[currentTask.job]) {
                        foundExecTask = true;

                        //Find the earliest time to execute the task
                        int execTime = currentTask.task == 0 ? timesMachineFree[m] : Integer.max(timesMachineFree[m], startTimes[currentTask.job][currentTask.task - 1] + instance.duration(currentTask.job, currentTask.task-1));
                        startTimes[currentTask.job][currentTask.task] = execTime;

                        //Update arrays
                        numberTaskByMachine[m]++;
                        timesMachineFree[m] = execTime + instance.duration(currentTask.job, currentTask.task);
                        jobNextTask[currentTask.job]++;
                    }

                }
                //If task not executable, check the next machine
                m++;
            }
        }

        // we exited the loop : all tasks have been scheduled successfully
        return new Schedule(instance, startTimes);
    }

    /** Creates an exact copy of this resource order. */
    public ResourceOrder copy() {
        return new ResourceOrder(this.toSchedule());
    }

    @Override
    public String toString()
    {
        StringBuilder s = new StringBuilder();
        for(int m=0; m < instance.numMachines; m++)
        {
            s.append("Machine ").append(m).append(" : ");
            for(int j=0; j<instance.numJobs; j++)
            {
                s.append(tasksByMachine[m][j]).append(" ; ");
            }
            s.append("\n");
        }

        return s.toString();
    }

}