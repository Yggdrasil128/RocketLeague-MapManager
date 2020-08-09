import org.junit.Assert;
import org.junit.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class InetAddressTest {
	@Test
	public void test() throws UnknownHostException {
		Assert.assertEquals(InetAddress.getByName("::1"), InetAddress.getByName("0:0:0:0:0:0:0:1"));

//		Gson gson = new GsonBuilder().create();
//		System.out.println(gson.toJson(InetAddress.getByName("::1")));
	}
}
