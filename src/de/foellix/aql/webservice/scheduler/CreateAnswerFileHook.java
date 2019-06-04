package de.foellix.aql.webservice.scheduler;

import java.io.File;

import de.foellix.aql.datastructure.Answer;
import de.foellix.aql.datastructure.App;
import de.foellix.aql.datastructure.Permission;
import de.foellix.aql.datastructure.Permissions;
import de.foellix.aql.datastructure.Reference;
import de.foellix.aql.datastructure.handler.AnswerHandler;
import de.foellix.aql.helper.Helper;
import de.foellix.aql.system.task.ITaskHook;
import de.foellix.aql.system.task.TaskInfo;
import de.foellix.aql.system.task.ToolTaskInfo;

public class CreateAnswerFileHook implements ITaskHook {
	@Override
	public void execute(TaskInfo taskinfo) {
		if (taskinfo instanceof ToolTaskInfo) {
			final Answer answer = new Answer();
			final Permission permission = new Permission();
			permission.setName("No permission! Just for file transfer!");
			final Reference reference = new Reference();
			final File apkFile = new File(
					((ToolTaskInfo) taskinfo).getQuestion().getAllReferences().iterator().next().getApp().getFile());
			final App app = Helper.createApp(apkFile);
			reference.setApp(app);
			permission.setReference(reference);
			answer.setPermissions(new Permissions());
			answer.getPermissions().getPermission().add(permission);

			String filename = taskinfo.getTool().getExecute().getResult();
			filename = Helper.replaceVariables(filename, taskinfo, app);

			AnswerHandler.createXML(answer, new File(filename));
		}
	}
}