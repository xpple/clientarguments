package dev.xpple.clientarguments.arguments;

import com.google.common.collect.Lists;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.EntitySelectorReader;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CMessageArgumentType implements ArgumentType<CMessageArgumentType.MessageFormat> {
	private static final Collection<String> EXAMPLES = Arrays.asList("Hello world!", "foo", "@e", "Hello @p :)");

	public CMessageArgumentType() {
	}

	public static CMessageArgumentType message() {
		return new CMessageArgumentType();
	}

	public static Text getCMessage(CommandContext<FabricClientCommandSource> context, String name) throws CommandSyntaxException {
		MessageFormat messageFormat = context.getArgument(name, MessageFormat.class);
		return messageFormat.format(context.getSource());
	}

	public MessageFormat parse(StringReader stringReader) throws CommandSyntaxException {
		return MessageFormat.parse(stringReader, true);
	}

	public Collection<String> getExamples() {
		return EXAMPLES;
	}

	public Text toText(MessageFormat messageFormat) {
		return Text.literal(messageFormat.getContents());
	}

	public CompletableFuture<Text> decorate(FabricClientCommandSource source, MessageFormat messageFormat) throws CommandSyntaxException {
		return messageFormat.decorate(source);
	}

	public Class<MessageFormat> getFormatClass() {
		return MessageFormat.class;
	}

	public static class MessageFormat {
		final String contents;
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

		CompletableFuture<Text> decorate(FabricClientCommandSource source) throws CommandSyntaxException {
			Text text = this.format(source);
			return CompletableFuture.completedFuture(text);
		}

		Text format(FabricClientCommandSource source) throws CommandSyntaxException {
			return this.format(source, source.hasPermissionLevel(2));
		}

		public Text format(FabricClientCommandSource source, boolean canUseSelectors) throws CommandSyntaxException {
			if (this.selectors.length != 0 && canUseSelectors) {
				MutableText mutableText = Text.literal(this.contents.substring(0, this.selectors[0].getStart()));
				int i = this.selectors[0].getStart();

				for(MessageSelector messageSelector : this.selectors) {
					Text text = messageSelector.format(source);
					if (i < messageSelector.getStart()) {
						mutableText.append(this.contents.substring(i, messageSelector.getStart()));
					}

					if (text != null) {
						mutableText.append(text);
					}

					i = messageSelector.getEnd();
				}

				if (i < this.contents.length()) {
					mutableText.append(this.contents.substring(i));
				}

				return mutableText;
			} else {
				return Text.literal(this.contents);
			}
		}

		public static MessageFormat parse(StringReader reader, boolean canUseSelectors) throws CommandSyntaxException {
			String string = reader.getString().substring(reader.getCursor(), reader.getTotalLength());
			if (!canUseSelectors) {
				reader.setCursor(reader.getTotalLength());
				return new MessageFormat(string, new MessageSelector[0]);
			} else {
				List<MessageSelector> list = Lists.newArrayList();
				int i = reader.getCursor();

				while(true) {
					int j;
					CEntitySelector entitySelector;
					while(true) {
						if (!reader.canRead()) {
							return new MessageFormat(string, list.toArray(new MessageSelector[0]));
						}

						if (reader.peek() == '@') {
							j = reader.getCursor();

							try {
								CEntitySelectorReader entitySelectorReader = new CEntitySelectorReader(reader);
								entitySelector = entitySelectorReader.read();
								break;
							} catch (CommandSyntaxException e) {
								if (e.getType() != CEntitySelectorReader.MISSING_EXCEPTION
										&& e.getType() != CEntitySelectorReader.UNKNOWN_SELECTOR_EXCEPTION) {
									throw e;
								}

								reader.setCursor(j + 1);
							}
						} else {
							reader.skip();
						}
					}

					list.add(new MessageSelector(j - i, reader.getCursor() - i, entitySelector));
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
