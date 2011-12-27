package rssminer.task;

public interface IBlockingTaskProvider {

    IHttpTask getTask(int timeout);

}
