package dev.xpple.clientarguments.arguments;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.nbt.CollectionTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.mutable.MutableBoolean;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class CNbtPathArgument implements ArgumentType<CNbtPathArgument.NbtPath> {
	private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo.bar", "foo[0]", "[0]", "[]", "{foo=bar}");
	public static final SimpleCommandExceptionType INVALID_PATH_NODE_EXCEPTION = new SimpleCommandExceptionType(Component.translatable("arguments.nbtpath.node.invalid"));
	public static final SimpleCommandExceptionType TOO_DEEP_EXCEPTION = new SimpleCommandExceptionType(Component.translatable("arguments.nbtpath.too_deep"));
	public static final DynamicCommandExceptionType NOTHING_FOUND_EXCEPTION = new DynamicCommandExceptionType(path -> Component.translatableEscape("arguments.nbtpath.nothing_found", path));
	static final DynamicCommandExceptionType EXPECTED_LIST_EXCEPTION = new DynamicCommandExceptionType(nbt -> Component.translatableEscape("commands.data.modify.expected_list", nbt));
	static final DynamicCommandExceptionType INVALID_INDEX_EXCEPTION = new DynamicCommandExceptionType(index -> Component.translatableEscape("commands.data.modify.invalid_index", index));

	public static CNbtPathArgument nbtPath() {
		return new CNbtPathArgument();
	}

	public static NbtPath getNbtPath(final CommandContext<FabricClientCommandSource> context, final String name) {
		return context.getArgument(name, NbtPath.class);
	}

	@Override
	public NbtPath parse(final StringReader stringReader) throws CommandSyntaxException {
		List<Node> list = Lists.newArrayList();
		int i = stringReader.getCursor();
		Object2IntMap<Node> object2IntMap = new Object2IntOpenHashMap<>();
		boolean bl = true;

		while (stringReader.canRead() && stringReader.peek() != ' ') {
			Node node = parseNode(stringReader, bl);
			list.add(node);
			object2IntMap.put(node, stringReader.getCursor() - i);
			bl = false;
			if (stringReader.canRead()) {
				char c = stringReader.peek();
				if (c != ' ' && c != '[' && c != '{') {
					stringReader.expect('.');
				}
			}
		}

		return new NbtPath(stringReader.getString().substring(i, stringReader.getCursor()), list.toArray(new Node[0]), object2IntMap);
	}

	private static Node parseNode(StringReader reader, boolean root) throws CommandSyntaxException {
        return switch (reader.peek()) {
            case '"', '\'' -> readCompoundChildNode(reader, reader.readString());
            case '[' -> {
                reader.skip();
                int i = reader.peek();
                if (i == 123) {
                    CompoundTag compoundTag2 = TagParser.parseCompoundAsArgument(reader);
                    reader.expect(']');
                    yield new MatchElementNode(compoundTag2);
                }
				if (i == 93) {
                    reader.skip();
                    yield AllElementsNode.INSTANCE;
                }
				int j = reader.readInt();
				reader.expect(']');
				yield new IndexedElementNode(j);
            }
            case '{' -> {
                if (!root) {
                    throw INVALID_PATH_NODE_EXCEPTION.createWithContext(reader);
                }

                CompoundTag compoundTag = TagParser.parseCompoundAsArgument(reader);
                yield new MatchRootObjectNode(compoundTag);
            }
            default -> readCompoundChildNode(reader, readName(reader));
        };
	}

	private static Node readCompoundChildNode(StringReader reader, String name) throws CommandSyntaxException {
		if (reader.canRead() && reader.peek() == '{') {
			CompoundTag compoundTag = TagParser.parseCompoundAsArgument(reader);
			return new MatchObjectNode(name, compoundTag);
		}
		return new CompoundChildNode(name);
	}

	private static String readName(StringReader reader) throws CommandSyntaxException {
		int i = reader.getCursor();

		while(reader.canRead() && isNameCharacter(reader.peek())) {
			reader.skip();
		}

		if (reader.getCursor() == i) {
			throw INVALID_PATH_NODE_EXCEPTION.createWithContext(reader);
		}
		return reader.getString().substring(i, reader.getCursor());
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}

	private static boolean isNameCharacter(char c) {
		return c != ' ' && c != '"' && c != '\'' && c != '[' && c != ']' && c != '.' && c != '{' && c != '}';
	}

	static Predicate<Tag> getPredicate(CompoundTag filter) {
		return nbt -> NbtUtils.compareNbt(filter, nbt, true);
	}

	static class AllElementsNode implements Node {
		public static final AllElementsNode INSTANCE = new AllElementsNode();

		private AllElementsNode() {
		}

		@Override
		public void get(Tag current, List<Tag> results) {
			if (current instanceof CollectionTag collectionTag) {
				Iterables.addAll(results, collectionTag);
			}
		}

		@Override
		public void getOrInit(Tag current, Supplier<Tag> source, List<Tag> results) {
			if (current instanceof CollectionTag collectionTag) {
				if (collectionTag.isEmpty()) {
					Tag nbtElement = source.get();
					if (collectionTag.addTag(0, nbtElement)) {
						results.add(nbtElement);
					}
				} else {
					Iterables.addAll(results, collectionTag);
				}
			}
		}

		@Override
		public Tag init() {
			return new ListTag();
		}

		@Override
		public int set(Tag current, Supplier<Tag> source) {
			if (!(current instanceof CollectionTag collectionTag)) {
				return 0;
			}
            int i = collectionTag.size();
			if (i == 0) {
				collectionTag.addTag(0, source.get());
				return 1;
			}
			Tag tag = source.get();
			int j = i - (int) collectionTag.stream().filter(tag::equals).count();
			if (j == 0) {
				return 0;
			}
			collectionTag.clear();
			if (!collectionTag.addTag(0, tag)) {
				return 0;
			}
			for (int k = 1; k < i; ++k) {
				collectionTag.addTag(k, source.get());
			}

			return j;
		}

		@Override
		public int clear(Tag current) {
			if (current instanceof CollectionTag collectionTag) {
				int i = collectionTag.size();
				if (i > 0) {
					collectionTag.clear();
					return i;
				}
			}

			return 0;
		}
	}

	static class MatchElementNode implements Node {
		private final CompoundTag filter;
		private final Predicate<Tag> predicate;

		public MatchElementNode(CompoundTag filter) {
			this.filter = filter;
			this.predicate = CNbtPathArgument.getPredicate(filter);
		}

		@Override
		public void get(Tag current, List<Tag> results) {
			if (current instanceof ListTag nbtList) {
				nbtList.stream().filter(this.predicate).forEach(results::add);
			}
		}

		@Override
		public void getOrInit(Tag current, Supplier<Tag> source, List<Tag> results) {
			MutableBoolean mutableBoolean = new MutableBoolean();
			if (current instanceof ListTag listTag) {
				listTag.stream().filter(this.predicate).forEach(nbt -> {
					results.add(nbt);
					mutableBoolean.setTrue();
				});
				if (mutableBoolean.isFalse()) {
					CompoundTag compoundTag = this.filter.copy();
					listTag.add(compoundTag);
					results.add(compoundTag);
				}
			}
		}

		@Override
		public Tag init() {
			return new ListTag();
		}

		@Override
		public int set(Tag current, Supplier<Tag> source) {
			int i = 0;
			if (current instanceof ListTag listTag) {
				int j = listTag.size();
				if (j == 0) {
					listTag.add(source.get());
					++i;
				} else {
					for (int k = 0; k < j; ++k) {
						Tag tag = listTag.get(k);
						if (this.predicate.test(tag)) {
							Tag tag2 = source.get();
							if (!tag2.equals(tag) && listTag.setTag(k, tag2)) {
								++i;
							}
						}
					}
				}
			}

			return i;
		}

		@Override
		public int clear(Tag current) {
			int i = 0;
			if (current instanceof ListTag listTag) {
				for (int j = listTag.size() - 1; j >= 0; --j) {
					if (this.predicate.test(listTag.get(j))) {
						listTag.remove(j);
						++i;
					}
				}
			}

			return i;
		}
	}

	static class MatchObjectNode implements Node {
		private final String name;
		private final CompoundTag filter;
		private final Predicate<Tag> predicate;

		public MatchObjectNode(String name, CompoundTag filter) {
			this.name = name;
			this.filter = filter;
			this.predicate = CNbtPathArgument.getPredicate(filter);
		}

		@Override
		public void get(Tag current, List<Tag> results) {
			if (current instanceof CompoundTag) {
				Tag tag = ((CompoundTag)current).get(this.name);
				if (this.predicate.test(tag)) {
					results.add(tag);
				}
			}
		}

		@Override
		public void getOrInit(Tag current, Supplier<Tag> source, List<Tag> results) {
			if (current instanceof CompoundTag compoundTag) {
				Tag tag = compoundTag.get(this.name);
				if (tag == null) {
					Tag tag2 = this.filter.copy();
					compoundTag.put(this.name, tag2);
					results.add(tag2);
				} else if (this.predicate.test(tag)) {
					results.add(tag);
				}
			}
		}

		@Override
		public Tag init() {
			return new CompoundTag();
		}

		@Override
		public int set(Tag current, Supplier<Tag> source) {
			if (current instanceof CompoundTag compoundTag) {
				Tag tag = compoundTag.get(this.name);
				if (this.predicate.test(tag)) {
					Tag tag2 = source.get();
					if (!tag2.equals(tag)) {
						compoundTag.put(this.name, tag2);
						return 1;
					}
				}
			}

			return 0;
		}

		@Override
		public int clear(Tag current) {
			if (current instanceof CompoundTag compoundTag) {
				Tag nbtElement = compoundTag.get(this.name);
				if (this.predicate.test(nbtElement)) {
					compoundTag.remove(this.name);
					return 1;
				}
			}

			return 0;
		}
	}

	static class MatchRootObjectNode implements Node {
		private final Predicate<Tag> matcher;

		public MatchRootObjectNode(CompoundTag filter) {
			this.matcher = CNbtPathArgument.getPredicate(filter);
		}

		@Override
		public void get(Tag current, List<Tag> results) {
			if (current instanceof CompoundTag && this.matcher.test(current)) {
				results.add(current);
			}
		}

		@Override
		public void getOrInit(Tag current, Supplier<Tag> source, List<Tag> results) {
			this.get(current, results);
		}

		@Override
		public Tag init() {
			return new CompoundTag();
		}

		@Override
		public int set(Tag current, Supplier<Tag> source) {
			return 0;
		}

		@Override
		public int clear(Tag current) {
			return 0;
		}
	}

	static class IndexedElementNode implements Node {
		private final int index;

		public IndexedElementNode(int index) {
			this.index = index;
		}

		@Override
		public void get(Tag current, List<Tag> results) {
			if (current instanceof CollectionTag collectionTag) {
				int i = collectionTag.size();
				int j = this.index < 0 ? i + this.index : this.index;
				if (0 <= j && j < i) {
					results.add(collectionTag.get(j));
				}
			}
		}

		@Override
		public void getOrInit(Tag current, Supplier<Tag> source, List<Tag> results) {
			this.get(current, results);
		}

		@Override
		public Tag init() {
			return new ListTag();
		}

		@Override
		public int set(Tag current, Supplier<Tag> source) {
			if (current instanceof CollectionTag collectionTag) {
				int i = collectionTag.size();
				int j = this.index < 0 ? i + this.index : this.index;
				if (0 <= j && j < i) {
					Tag tag = collectionTag.get(j);
					Tag tag2 = source.get();
					if (!tag2.equals(tag) && collectionTag.setTag(j, tag2)) {
						return 1;
					}
				}
			}

			return 0;
		}

		@Override
		public int clear(Tag current) {
			if (current instanceof CollectionTag collectionTag) {
				int i = collectionTag.size();
				int j = this.index < 0 ? i + this.index : this.index;
				if (0 <= j && j < i) {
					collectionTag.remove(j);
					return 1;
				}
			}

			return 0;
		}
	}

	static class CompoundChildNode implements Node {
		private final String name;

		public CompoundChildNode(String name) {
			this.name = name;
		}

		@Override
		public void get(Tag current, List<Tag> results) {
			if (current instanceof CompoundTag) {
				Tag tag = ((CompoundTag)current).get(this.name);
				if (tag != null) {
					results.add(tag);
				}
			}
		}

		@Override
		public void getOrInit(Tag current, Supplier<Tag> source, List<Tag> results) {
			if (current instanceof CompoundTag compoundTag) {
				Tag tag;
				if (compoundTag.contains(this.name)) {
					tag = compoundTag.get(this.name);
				} else {
					tag = source.get();
					compoundTag.put(this.name, tag);
				}

				results.add(tag);
			}
		}

		@Override
		public Tag init() {
			return new CompoundTag();
		}

		@Override
		public int set(Tag current, Supplier<Tag> source) {
			if (current instanceof CompoundTag compoundTag) {
				Tag tag = source.get();
				Tag tag2 = compoundTag.put(this.name, tag);
				if (!tag.equals(tag2)) {
					return 1;
				}
			}

			return 0;
		}

		@Override
		public int clear(Tag current) {
			if (current instanceof CompoundTag compoundTag && compoundTag.contains(this.name)) {
				compoundTag.remove(this.name);
				return 1;
			}

			return 0;
		}
	}

	public static class NbtPath {
		private final String string;
		private final Object2IntMap<Node> nodeEndIndices;
		private final Node[] nodes;
		public static final Codec<NbtPath> CODEC = Codec.STRING.comapFlatMap(path -> {
			try {
				NbtPath nbtPath = new CNbtPathArgument().parse(new StringReader(path));
				return DataResult.success(nbtPath);
			} catch (CommandSyntaxException var2) {
				return DataResult.error(() -> "Failed to parse path " + path + ": " + var2.getMessage());
			}
		}, NbtPath::getString);

		public static NbtPath parse(String path) throws CommandSyntaxException {
			return new CNbtPathArgument().parse(new StringReader(path));
		}

		public NbtPath(String string, Node[] nodes, Object2IntMap<Node> nodeEndIndices) {
			this.string = string;
			this.nodes = nodes;
			this.nodeEndIndices = nodeEndIndices;
		}

		public List<Tag> get(Tag tag) throws CommandSyntaxException {
			List<Tag> list = Collections.singletonList(tag);

			for(Node node : this.nodes) {
				list = node.get(list);
				if (list.isEmpty()) {
					throw this.createNothingFoundException(node);
				}
			}

			return list;
		}

		public int count(Tag tag) {
			List<Tag> list = Collections.singletonList(tag);

			for (Node node : this.nodes) {
				list = node.get(list);
				if (list.isEmpty()) {
					return 0;
				}
			}

			return list.size();
		}

		private List<Tag> getTerminals(Tag start) throws CommandSyntaxException {
			List<Tag> list = Collections.singletonList(start);

			for (int i = 0; i < this.nodes.length - 1; ++i) {
				Node node = this.nodes[i];
				int j = i + 1;
				list = node.getOrInit(list, this.nodes[j]::init);
				if (list.isEmpty()) {
					throw this.createNothingFoundException(node);
				}
			}

			return list;
		}

		public List<Tag> getOrInit(Tag tag, Supplier<Tag> source) throws CommandSyntaxException {
			List<Tag> list = this.getTerminals(tag);
			Node node = this.nodes[this.nodes.length - 1];
			return node.getOrInit(list, source);
		}

		private static int forEach(List<Tag> tags, Function<Tag, Integer> operation) {
			return tags.stream().map(operation).reduce(0, Integer::sum);
		}

		public static boolean isTooDeep(Tag tag, int depth) {
			if (depth >= 512) {
				return true;
			}
			if (tag instanceof CompoundTag compoundTag) {
				for (Tag nbtElement : compoundTag.values()) {
					if (nbtElement != null && isTooDeep(nbtElement, depth + 1)) {
						return true;
					}
				}
			} else if (tag instanceof ListTag) {
				for (Tag tag2 : (ListTag)tag) {
					if (isTooDeep(tag2, depth + 1)) {
						return true;
					}
				}
			}

			return false;
		}

		public int put(Tag tag, Tag source) throws CommandSyntaxException {
			if (isTooDeep(source, this.getDepth())) {
				throw CNbtPathArgument.TOO_DEEP_EXCEPTION.create();
			}
			Tag nbtElement = source.copy();
			List<Tag> list = this.getTerminals(tag);
			if (list.isEmpty()) {
				return 0;
			}
			Node node = this.nodes[this.nodes.length - 1];
			MutableBoolean mutableBoolean = new MutableBoolean(false);
			return forEach(list, nbt -> node.set(nbt, () -> {
				if (mutableBoolean.isFalse()) {
					mutableBoolean.setTrue();
					return nbtElement;
				}
				return nbtElement.copy();
			}));
		}

		private int getDepth() {
			return this.nodes.length;
		}

		public int insert(int index, CompoundTag compound, List<Tag> tags) throws CommandSyntaxException {
			List<Tag> list = new ArrayList<>(tags.size());

			for (Tag tag : tags) {
				Tag tag2 = tag.copy();
				list.add(tag2);
				if (isTooDeep(tag2, this.getDepth())) {
					throw CNbtPathArgument.TOO_DEEP_EXCEPTION.create();
				}
			}

			Collection<Tag> collection = this.getOrInit(compound, ListTag::new);
			int i = 0;
			boolean bl = false;

			for (Tag tag3 : collection) {
				if (!(tag3 instanceof CollectionTag collectionTag)) {
					throw CNbtPathArgument.EXPECTED_LIST_EXCEPTION.create(tag3);
				}

                boolean bl2 = false;
				int j = index < 0 ? collectionTag.size() + index + 1 : index;

				for (Tag tag4 : list) {
					try {
						if (collectionTag.addTag(j, bl ? tag4.copy() : tag4)) {
							++j;
							bl2 = true;
						}
					} catch (IndexOutOfBoundsException var16) {
						throw CNbtPathArgument.INVALID_INDEX_EXCEPTION.create(j);
					}
				}

				bl = true;
				i += bl2 ? 1 : 0;
			}

			return i;
		}

		public int remove(Tag tag) {
			List<Tag> list = Collections.singletonList(tag);

			for (int i = 0; i < this.nodes.length - 1; ++i) {
				list = this.nodes[i].get(list);
			}

			Node node = this.nodes[this.nodes.length - 1];
			return forEach(list, node::clear);
		}

		private CommandSyntaxException createNothingFoundException(Node node) {
			int i = this.nodeEndIndices.getInt(node);
			return CNbtPathArgument.NOTHING_FOUND_EXCEPTION.create(this.string.substring(0, i));
		}

		public String toString() {
			return this.string;
		}

		public String getString() {
			return this.string;
		}
	}

	interface Node {
		void get(Tag current, List<Tag> results);

		void getOrInit(Tag current, Supplier<Tag> source, List<Tag> results);

		Tag init();

		int set(Tag current, Supplier<Tag> source);

		int clear(Tag current);

		default List<Tag> get(List<Tag> tags) {
			return this.process(tags, this::get);
		}

		default List<Tag> getOrInit(List<Tag> tags, Supplier<Tag> supplier) {
			return this.process(tags, (current, results) -> this.getOrInit(current, supplier, results));
		}

		default List<Tag> process(List<Tag> tags, BiConsumer<Tag, List<Tag>> action) {
			List<Tag> list = Lists.newArrayList();

			for(Tag tag : tags) {
				action.accept(tag, list);
			}

			return list;
		}
	}
}
