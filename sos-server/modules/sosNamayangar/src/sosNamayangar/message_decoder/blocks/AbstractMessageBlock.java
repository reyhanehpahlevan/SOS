package sosNamayangar.message_decoder.blocks;

import sosNamayangar.message_decoder.MessageConstants;
import sosNamayangar.message_decoder.ReadXml;


/**
 * 
 * @author Ali
 * 
 */
public abstract class AbstractMessageBlock implements SOSMessageBlock {
	protected String header = null;
	protected DataArrayList data;
	
	// protected int hashCode;
//	private SOSLoggerSystem sosAbstractMessageBlocklogger;

	public AbstractMessageBlock() {
	//	sosAbstractMessageBlocklogger = new SOSLoggerSystem(null, "sosAbstractMessageBlocklogger", true, OutputType.File);
//		sosAbstractMessageBlocklogger.setFullLoggingLevel();
	}

	public AbstractMessageBlock(String header) {
		try{
		data = new DataArrayList(ReadXml.blocks.get(header).data().size());
		}catch (Exception e) {
				System.out.println("heade "+header);
//			System.exit(1);
				e.printStackTrace();
		}
		setHeader(header);
//		sosAbstractMessageBlocklogger = new SOSLoggerSystem(null, "sosAbstractMessageBlocklogger", true, OutputType.File);
//		sosAbstractMessageBlocklogger.setFullLoggingLevel();
	}

	public AbstractMessageBlock(AbstractMessageBlock messageBlock) {
		data = messageBlock.getData();
		this.header = messageBlock.getHeader();
//		sosAbstractMessageBlocklogger = new SOSLoggerSystem(null, "sosAbstractMessageBlocklogger", true, OutputType.File);
//		sosAbstractMessageBlocklogger.setFullLoggingLevel();
	}

	public AbstractMessageBlock(String header, DataArrayList data) {
		this.data = data;
		this.header = header;
//		sosAbstractMessageBlocklogger = new SOSLoggerSystem(null, "sosAbstractMessageBlocklogger", true, OutputType.File);
//		sosAbstractMessageBlocklogger.setFullLoggingLevel();
	}

	/**
	 * you can put data(key,value) and get(key) <b>important!: key should be
	 * unique </b>
	 */
	@Override
	public void addData(String key, int value) {
		data.put(key, value);
	}

	@Override
	public DataArrayList getData() {
		return data;
	}
	
	// @Override
	// public Set<Entry<String, Integer>> getAllData() {
	// return data.entrySet();
	// }

	@Override
	public int getData(String key) {
		try {
			if (data.get(key) < 0) {
				// sosAbstractMessageBlocklogger.error(new Error("Invalid key \" " + key + "\" in header: " + header));
				return -1;
			}
		} catch (Exception e) {
			return -1;
		}
		return data.get(key);
	}

	@Override
	public String getHeader() {
		return header;
	}

	@Override
	public Integer getKeyData() {
		return getData(ReadXml.blocks.get(header).getDataKey());
	}

	@Override
	public void setHeader(String header) {
		this.header = header;
	}

	@Override
	public String toString() {
		return "Header:" + header + " data:[" + data.toString() + "] ";
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SOSMessageBlock) {
			SOSMessageBlock m2 = (SOSMessageBlock) obj;
			if (m2.hashCode() == this.hashCode())
				return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		// if (hashCode == 0) {
		// hashCode =getHash(header, data);
		// }
		return getHash(header, data);

	}

	public static int getHash(String header, DataArrayList data) {
		try {
			if (ReadXml.blocks.get(header).getDataKey() == null) {
				StringBuffer sb = new StringBuffer(header);
				for (int data0 : data.values()) {
					sb.append(data0);
				}
				return sb.toString().hashCode();
			} else {
				return (ReadXml.headerToIndex(header) << (31 - MessageConstants.HEADER_SIZE)) + data.get(ReadXml.blocks.get(header).getDataKey());
			}
		} catch (Exception e) {// XXX niyazi be in try catch nist vali ehtiat e dige ;)
			new Error("What happen??? how this block(" + header + ")added????").printStackTrace();
			return ("header" + Math.random()).hashCode();
		}
	}
}
