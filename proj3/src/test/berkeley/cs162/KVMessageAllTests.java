package test.berkeley.cs162;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ KVMessageInputStreamTest.class, KVMessageSendMessageTest.class,
		KVMessageStringStringTest.class, KVMessageStringTest.class,
		KVMessageToXMLTest.class })
public class KVMessageAllTests {

}
