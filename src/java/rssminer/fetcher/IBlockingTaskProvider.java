/*
 * Copyright (c) Feng Shen<shenedu@gmail.com>. All rights reserved.
 * You must not remove this notice, or any other, from this software.
 */

package rssminer.fetcher;

public interface IBlockingTaskProvider {

    IHttpTask getTask(int timeout);

}
