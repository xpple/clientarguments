package dev.xpple.clientarguments.arguments;

import com.google.common.collect.Lists;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.argument.SignedArgumentType;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class CMessageArgumentType implements SignedArgumentType<CMessageArgumentType.MessageFormat> {
	private static final Collection<String> EXAMPLES = Arrays.asList("Hello world!", "foo", "@e", "Hello @p :)");
	static final Dynamic2CommandExceptionType MESSAGE_TOO_LONG_EXCEPTION = new Dynamic2CommandExceptionType((length, maxLength) -> Text.stringifiedTranslatable("argument.message.too_long", length, maxLength));

	public static CMessageArgumentType message() {
		return new CMessageArgumentType();
	}

	public static Text getMessage(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
		MessageFormat messageFormat = context.getArgument(name, MessageFormat.class);
		return messageFormat.format(context.getSource());
	}

	@Override
	public MessageFormat parse(StringReader stringReader) throws CommandSyntaxException {
		return MessageFormat.parse(stringReader, true);
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}

	public record MessageFormat(String contents, MessageSelector[] selectors) {
		Text format(FabricClientCommandSource source) throws CommandSyntaxException {
			return this.format(source, source.hasPermissionLevel(2));
		}

		public Text format(FabricClientCommandSource source, boolean canUseSelectors) throws CommandSyntaxException {
            if (this.selectors.length == 0 || !canUseSelectors) {
                return Text.literal(this.contents);
            }
            MutableText mutableText = Text.literal(this.contents.substring(0, this.selectors[0].start()));
            int i = this.selectors[0].start();

            for (MessageSelector messageSelector : this.selectors) {
                Text text = messageSelector.format(source);
                if (i < messageSelector.start()) {
                    mutableText.append(this.contents.substring(i, messageSelector.start()));
                }

                mutableText.append(text);
                i = messageSelector.end();
            }

            if (i < this.contents.length()) {
                mutableText.append(this.contents.substring(i));
            }

            return mutableText;
        }

		public static MessageFormat parse(StringReader reader, boolean canUseSelectors) throws CommandSyntaxException {
			if (reader.getRemainingLength() > 256) {
				throw CMessageArgumentType.MESSAGE_TOO_LONG_EXCEPTION.create(reader.getRemainingLength(), 256);
			}
			String string = reader.getRemaining();
			if (!canUseSelectors) {
				reader.setCursor(reader.getTotalLength());
				return new MessageFormat(string, new MessageSelector[0]);
			}
			List<MessageSelector> list = Lists.newArrayList();
			int i = reader.getCursor();

			while (true) {
				int j;
				CEntitySelector entitySelector;
				while (true) {
					if (!reader.canRead()) {
						return new MessageFormat(string, list.toArray(new MessageSelector[0]));
					}

					if (reader.peek() == '@') {
						j = reader.getCursor();

						try {
							CEntitySelectorReader entitySelectorReader = new CEntitySelectorReader(reader);
							entitySelector = entitySelectorReader.read();
							break;
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

				list.add(new MessageSelector(j - i, reader.getCursor() - i, entitySelector));
			}
		}
	}

	public record MessageSelector(int start, int end, CEntitySelector selector) {
		public Text format(FabricClientCommandSource source) throws CommandSyntaxException {
			return CEntitySelector.getNames(this.selector.getEntities(source));
		}
	}
}
