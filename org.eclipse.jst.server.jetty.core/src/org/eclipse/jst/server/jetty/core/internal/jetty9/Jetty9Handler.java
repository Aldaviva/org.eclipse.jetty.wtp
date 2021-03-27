package org.eclipse.jst.server.jetty.core.internal.jetty9;

import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jst.server.jetty.core.internal.jetty8.Jetty8Handler;
import org.eclipse.wst.server.core.IModule;

public class Jetty9Handler extends Jetty8Handler {

	@Override
	public IStatus canAddModule(IModule module) {
		String version = module.getModuleType().getVersion();
		
		if("3.1".equals(version)){
			return Status.OK_STATUS;
		}
		
		return super.canAddModule(module);
	}
	
	@Override
	public List<String> getRuntimeVMArguments(IPath installPath, IPath configPath, IPath deployPath, int mainPort, int adminPort, boolean isTestEnv)
    {
		List<String> vmArguments = getCommonRuntimeVMArguments(installPath, configPath, deployPath, mainPort, adminPort, isTestEnv);
        
        if(isTestEnv){
        	vmArguments.add("-Djetty.base=\""+configPath.toOSString()+"\"");
        	vmArguments.add("-Djetty.home=\""+installPath.toOSString()+"\"");
        } else {
        	vmArguments.add("-Djetty.base=\"" + installPath.toOSString() + "\"");
        	vmArguments.add("-Djetty.home=\"" + installPath.toOSString() + "\"");
        }
    	
		return vmArguments;
    }
	
}
