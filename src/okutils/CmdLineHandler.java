package OKUtils;

import java.util.*;
// Supported cmd line format: "-<switch> [optional_param_list]"+
// 'switch' is the key; param_list - an entry in the form of array
public class CmdLineHandler
{
	protected HashMap switchMap;
	/** Fills the switch map out of 'args' array */
	public CmdLineHandler(String[] args)
	{
		switchMap = new HashMap();
		String lastSwitch = null;
		int paramCnt = 0;
		for (  int i = 0;  i < args.length;  i++  )
		{
			String token = args[i];
			if ( is_switch(token) )
			{
				// finalize the prev. switch and init the new param list
				if ( (lastSwitch != null) )
				{
					String[] paramArr = put_range_into_array(args,
														i-paramCnt, paramCnt);
					switchMap.put(lastSwitch, paramArr);								   
				}
				lastSwitch = token;
				paramCnt = 0;
			}
			else
			{
				if ( lastSwitch != null )
				{
					++paramCnt;
				}
				else
				{
					// a primitive command line - no switches; create "" switch
					lastSwitch = new String("");
				}
			}
		}
		// store the last switch
		String[] paramArr = put_range_into_array(args,
											args.length-paramCnt, paramCnt);
		switchMap.put(lastSwitch, paramArr);								   
	}

	public boolean switch_appeares(String sw)
	{
		return  switchMap.containsKey(sw);
	}
	
	public String[] params_of_switch(String sw)
	{
		if ( switchMap.containsKey(sw) )
		{
			String[] paramArr = (String[])switchMap.get(sw);
			return  paramArr;
		}
		else
			return  null;
	}

	public String toString()
	{
		String str = new String();
		Set allMappings = switchMap.entrySet();
		Iterator it = allMappings.iterator();
		for (  ; it.hasNext();  )
		{ 
			Map.Entry entry = (Map.Entry)it.next(); 
            String sw			= (String  )entry.getKey(); 
            String[] paramArr	= (String[])entry.getValue(); 
			str  += sw + " => {";
			if ( paramArr != null )
				for (  int i = 0;  i < paramArr.length;  i++  )
					str += ((i!=0)? " ":"") + paramArr[i];
			str += "}  ";
        }
		return str;
	}
	
	protected static String[] put_range_into_array(String[] src, int startInd, int count)
	{
		String[] paramArr = (count != 0)? new String[count] : null;
		for (  int i = startInd;  i < startInd+count;  i++  )
			paramArr[i-startInd] = src[i];
		return  paramArr;
	}
	
	protected boolean is_switch(String token)
	{
		if ( (token.length() > 1) && (token.charAt(0) == '-') )
			return  true;
		else
			return  false;
	}
	
	public static void main(String[] args)
	{
		CmdLineHandler cmdLine = new CmdLineHandler(args);
		System.out.println(cmdLine);
	}
}

