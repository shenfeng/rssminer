package rssminer.fetcher;

public interface IBlockingTaskProvider {

    IHttpTask getTask(int timeout);

}
