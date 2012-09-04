package rssminer.classfier;

import java.util.Arrays;

import org.junit.Test;

public class DaemonTest {

	@Test
	public void testArrayToString() {
		double []ds = new double[] { 1.111, 2.1212 };
		String s = Arrays.toString(ds);
		System.out.println(s);
		
		System.out.println(ds[0] + ", " + ds[1]);
	}
}
