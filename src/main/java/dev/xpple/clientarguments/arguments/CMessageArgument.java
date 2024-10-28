package dev.xpple.clientarguments.arguments;

import com.google.common.collect.Lists;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.arguments.SignedArgument;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class CMessageArgument implements SignedArgument<CMessageArgument.Message> {
	private static final Collection<String> EXAMPLES = Arrays.asList("Hello world!", "foo", "@e", "Hello @p :)");
	static final Dynamic2CommandExceptionType TOO_LONG = new Dynamic2CommandExceptionType((length, maxLength) -> Component.translatableEscape("argument.message.too_long", length, maxLength));

	public static CMessageArgument message() {
		return new CMessageArgument();
	}

	public static Component getMessage(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
		CMessageArgument.Message message = context.getArgument(name, Message.class);
		return message.resolveComponent(context.getSource());
	}

	public CMessageArgument.Message parse(final StringReader stringReader) throws CommandSyntaxException {
		return CMessageArgument.Message.parseText(stringReader, true);
	}

	public <S> CMessageArgument.Message parse(final StringReader stringReader, final @Nullable S source) throws CommandSyntaxException {
		return CMessageArgument.Message.parseText(stringReader, CEntitySelectorParser.allowSelectors(source));
	}

	public Collection<String> getExamples() {
		return EXAMPLES;
	}

	public record Message(String text, CMessageArgument.Part[] parts) {

		Component resolveComponent(FabricClientCommandSource fabricClientCommandSource) throws CommandSyntaxException {
			return this.toComponent(fabricClientCommandSource, CEntitySelectorParser.allowSelectors(fabricClientCommandSource));
		}

		public Component toComponent(FabricClientCommandSource fabricClientCommandSource, boolean allowSelectors) throws CommandSyntaxException {
            if (this.parts.length == 0 || !allowSelectors) {
                return Component.literal(this.text);
            }

			MutableComponent mutableComponent = Component.literal(this.text.substring(0, this.parts[0].start()));
			int i = this.parts[0].start();
			for (Part part : this.parts) {
				Component component = part.toComponent(fabricClientCommandSource);
				if (i < part.start()) {
					mutableComponent.append(this.text.substring(i, part.start()));
				}
				mutableComponent.append(component);
				i = part.end();
			}

			if (i < this.text.length()) {
				mutableComponent.append(this.text.substring(i));
			}

			return mutableComponent;
        }

		public static CMessageArgument.Message parseText(StringReader stringReader, boolean allowSelectors) throws CommandSyntaxException {
			if (stringReader.getRemainingLength() > 256) {
				throw CMessageArgument.TOO_LONG.create(stringReader.getRemainingLength(), 256);
			}
			String string = stringReader.getRemaining();
			if (!allowSelectors) {
				stringReader.setCursor(stringReader.getTotalLength());
				return new CMessageArgument.Message(string, new CMessageArgument.Part[0]);
			}
			List<CMessageArgument.Part> list = Lists.newArrayList();
			int cursor = stringReader.getCursor();
			while (true) {
				int j;
				CEntitySelector entitySelector;
				while (true) {
					if (!stringReader.canRead()) {
						return new CMessageArgument.Message(string, list.toArray(new CMessageArgument.Part[0]));
					}
					if (stringReader.peek() == CEntitySelectorParser.SYNTAX_SELECTOR_START) {
						j = stringReader.getCursor();

						try {
							CEntitySelectorParser entitySelectorParser = new CEntitySelectorParser(stringReader, true);
							entitySelector = entitySelectorParser.parse();
							break;
						} catch (CommandSyntaxException var8) {
							if (var8.getType() != CEntitySelectorParser.ERROR_MISSING_SELECTOR_TYPE
								&& var8.getType() != CEntitySelectorParser.ERROR_UNKNOWN_SELECTOR_TYPE) {
								throw var8;
							}
							stringReader.setCursor(j + 1);
						}
					} else {
						stringReader.skip();
					}
				}
				list.add(new CMessageArgument.Part(j - cursor, stringReader.getCursor() - cursor, entitySelector));
			}
		}
	}

	public record Part(int start, int end, CEntitySelector selector) {
		public Component toComponent(FabricClientCommandSource fabricClientCommandSource) throws CommandSyntaxException {
			return CEntitySelector.joinNames(this.selector.findEntities(fabricClientCommandSource));
		}
	}
}
