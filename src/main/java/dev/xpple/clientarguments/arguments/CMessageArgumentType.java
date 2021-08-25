package dev.xpple.clientarguments.arguments;

import com.google.common.collect.Lists;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class CMessageArgumentType implements ArgumentType<CMessageArgumentType.MessageFormat> {

	private static final Collection<String> EXAMPLES = Arrays.asList("Hello world!", "foo", "@e", "Hello @p :)");

	public static CMessageArgumentType message() {
		return new CMessageArgumentType();
	}

	public static Text getCMessage(CommandContext<FabricClientCommandSource> command, String name) throws CommandSyntaxException {
		return command.getArgument(name, MessageFormat.class).format(command.getSource(), true);
	}

	@Override
	public MessageFormat parse(final StringReader stringReader) throws CommandSyntaxException {
		return MessageFormat.parse(stringReader, true);
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}

	public static class MessageFormat {
		private final String contents;
		private final MessageSelector[] selectors;

		public MessageFormat(String contents, MessageSelector[] selectors) {
			this.contents = contents;
			this.selectors = selectors;
		}

		public String getContents() {
			return this.contents;
		}

		public MessageSelector[] getSelectors() {
			return this.selectors;
		}

		public Text format(FabricClientCommandSource source, boolean canUseSelectors) throws CommandSyntaxException {
			if (this.selectors.length != 0 && canUseSelectors) {
				MutableText mutableText = new LiteralText(this.contents.substring(0, this.selectors[0].getStart()));
				int start = this.selectors[0].getStart();

				for (MessageSelector messageSelector : this.selectors) {
					Text text = messageSelector.format(source);
					if (start < messageSelector.getStart()) {
						mutableText.append(this.contents.substring(start, messageSelector.getStart()));
					}

					if (text != null) {
						mutableText.append(text);
					}

					start = messageSelector.getEnd();
				}

				if (start < this.contents.length()) {
					mutableText.append(this.contents.substring(start, this.contents.length()));
				}

				return mutableText;
			} else {
				return new LiteralText(this.contents);
			}
		}

		public static MessageFormat parse(StringReader reader, boolean canUseSelectors) throws CommandSyntaxException {
			// TODO: 23-8-2021 refactor
			String string = reader.getString().substring(reader.getCursor(), reader.getTotalLength());
			if (!canUseSelectors) {
				reader.setCursor(reader.getTotalLength());
				return new MessageFormat(string, new MessageSelector[0]);
			} else {
				List<MessageSelector> list = Lists.newArrayList();
				int cursor = reader.getCursor();

				while(true) {
					int j;
					CEntitySelector entitySelector2;
					label38:
					while(true) {
						while(reader.canRead()) {
							if (reader.peek() == '@') {
								j = reader.getCursor();

								try {
									CEntitySelectorReader entitySelectorReader = new CEntitySelectorReader(reader);
									entitySelector2 = entitySelectorReader.read();
									break label38;
								} catch (CommandSyntaxException var8) {
									if (var8.getType() != CEntitySelectorReader.MISSING_EXCEPTION && var8.getType() != CEntitySelectorReader.UNKNOWN_SELECTOR_EXCEPTION) {
										throw var8;
									}

									reader.setCursor(j + 1);
								}
							} else {
								reader.skip();
							}
						}

						return new MessageFormat(string, list.toArray(new MessageSelector[0]));
					}

					list.add(new MessageSelector(j - cursor, reader.getCursor() - cursor, entitySelector2));
				}
			}
		}
	}

	public static class MessageSelector {
		private final int start;
		private final int end;
		private final CEntitySelector selector;

		public MessageSelector(int start, int end, CEntitySelector selector) {
			this.start = start;
			this.end = end;
			this.selector = selector;
		}

		public int getStart() {
			return this.start;
		}

		public int getEnd() {
			return this.end;
		}

		public CEntitySelector getSelector() {
			return this.selector;
		}

		@Nullable
		public Text format(FabricClientCommandSource source) throws CommandSyntaxException {
			return CEntitySelector.getNames(this.selector.getEntities(source));
		}
	}
}
