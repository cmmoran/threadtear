package me.nov.threadtear.execution.cleanup.remove;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

import me.nov.threadtear.asm.Clazz;
import me.nov.threadtear.asm.util.Instructions;
import me.nov.threadtear.asm.util.Sandbox;
import me.nov.threadtear.execution.Execution;
import me.nov.threadtear.execution.ExecutionCategory;

public class RemoveUnnecessary extends Execution {

	public RemoveUnnecessary() {
		super(ExecutionCategory.CLEANING, "Remove unnecessary instructions",
				"Remove unnecessary instructions (even bad number or flow obfuscation).");
	}

	@Override
	public boolean execute(ArrayList<Clazz> classes, boolean verbose, boolean ignoreErr) {
		logger.info("Simulating stack for every method!");
		classes.stream().map(c -> c.node.methods).flatMap(List::stream).forEach(m -> {
			m.instructions = simulateAndRewrite(m, m.instructions);
		});
		return false;
	}

	private InsnList simulateAndRewrite(MethodNode m, InsnList instructions) {
		InsnList newInstructions = new InsnList();
		CodeOnStack codeOnStack = new CodeOnStack(new InsnList());
		for (AbstractInsnNode ain : instructions) {
			if (Instructions.computable(ain)) {
				codeOnStack.code.add(ain);
			} else {
				newInstructions.add(codeOnStack.compute());
				newInstructions.add(ain);
				codeOnStack.code.clear();
			}
		}
		// TODO very much to do here...
		return newInstructions;
	}

	public static class CodeOnStack {
		public InsnList code;

		public CodeOnStack(InsnList code) {
			this.code = code;
		}

		public boolean isComputable() {
			return StreamSupport.stream(code.spliterator(), false).allMatch(Instructions::computable);
		}

		public AbstractInsnNode compute() {
			StreamSupport.stream(code.spliterator(), false).filter(Instructions::unnecessaryToStack).forEach(code::remove);
			Object o = Sandbox.compute(code);
			return new LdcInsnNode(o);
		}
	}
}