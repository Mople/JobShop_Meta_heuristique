package jobshop;

public class ResourceTuple {

    public int job;
    public int task;

    public ResourceTuple(){
        job=-1;
        task=-1;
    }

    public void setJob(int job){
        this.job=job;
    }

    public void setTask(int task){
        this.task=task;
    }
}
