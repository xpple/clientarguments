package dev.xpple.clientarguments.arguments;

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
import net.minecraft.nbt.*;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import org.apache.commons.lang3.mutable.MutableBoolean;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class CNbtPathArgumentType implements ArgumentType<CNbtPathArgumentType.NbtPath> {
	private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo.bar", "foo[0]", "[0]", "[]", "{foo=bar}");
	public static final SimpleCommandExceptionType INVALID_PATH_NODE_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("arguments.nbtpath.node.invalid"));
	public static final SimpleCommandExceptionType TOO_DEEP_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("arguments.nbtpath.too_deep"));
	public static final DynamicCommandExceptionType NOTHING_FOUND_EXCEPTION = new DynamicCommandExceptionType(path -> Text.stringifiedTranslatable("arguments.nbtpath.nothing_found", path));
	static final DynamicCommandExceptionType EXPECTED_LIST_EXCEPTION = new DynamicCommandExceptionType(nbt -> Text.stringifiedTranslatable("commands.data.modify.expected_list", nbt));
	static final DynamicCommandExceptionType INVALID_INDEX_EXCEPTION = new DynamicCommandExceptionType(index -> Text.stringifiedTranslatable("commands.data.modify.invalid_index", index));

	public static CNbtPathArgumentType nbtPath() {
		return new CNbtPathArgumentType();
	}

	public static NbtPath getNbtPath(final CommandContext<FabricClientCommandSource> context, final String name) {
		return context.getArgument(name, NbtPath.class);
	}

	@Override
	public NbtPath parse(final StringReader stringReader) throws CommandSyntaxException {
		List<PathNode> list = Lists.newArrayList();
		int i = stringReader.getCursor();
		Object2IntMap<PathNode> object2IntMap = new Object2IntOpenHashMap<>();
		boolean bl = true;

		while (stringReader.canRead() && stringReader.peek() != ' ') {
			PathNode pathNode = parseNode(stringReader, bl);
			list.add(pathNode);
			object2IntMap.put(pathNode, stringReader.getCursor() - i);
			bl = false;
			if (stringReader.canRead()) {
				char c = stringReader.peek();
				if (c != ' ' && c != '[' && c != '{') {
					stringReader.expect('.');
				}
			}
		}

		return new NbtPath(stringReader.getString().substring(i, stringReader.getCursor()), list.toArray(new PathNode[0]), object2IntMap);
	}

	private static PathNode parseNode(StringReader reader, boolean root) throws CommandSyntaxException {
        return switch (reader.peek()) {
            case '"', '\'' -> readCompoundChildNode(reader, reader.readString());
            case '[' -> {
                reader.skip();
                int i = reader.peek();
                if (i == 123) {
                    NbtCompound nbtCompound2 = new StringNbtReader(reader).parseCompound();
                    reader.expect(']');
                    yield new FilteredListElementNode(nbtCompound2);
                }
				if (i == 93) {
                    reader.skip();
                    yield AllListElementNode.INSTANCE;
                }
				int j = reader.readInt();
				reader.expect(']');
				yield new IndexedListElementNode(j);
            }
            case '{' -> {
                if (!root) {
                    throw INVALID_PATH_NODE_EXCEPTION.createWithContext(reader);
                }

                NbtCompound nbtCompound = new StringNbtReader(reader).parseCompound();
                yield new FilteredRootNode(nbtCompound);
            }
            default -> readCompoundChildNode(reader, readName(reader));
        };
	}

	private static PathNode readCompoundChildNode(StringReader reader, String name) throws CommandSyntaxException {
		if (reader.canRead() && reader.peek() == '{') {
			NbtCompound nbtCompound = new StringNbtReader(reader).parseCompound();
			return new CNbtPathArgumentType.FilteredNamedNode(name, nbtCompound);
		}
		return new CNbtPathArgumentType.NamedNode(name);
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

	static Predicate<NbtElement> getPredicate(NbtCompound filter) {
		return nbt -> NbtHelper.matches(filter, nbt, true);
	}

	static class AllListElementNode implements PathNode {
		public static final CNbtPathArgumentType.AllListElementNode INSTANCE = new CNbtPathArgumentType.AllListElementNode();

		private AllListElementNode() {
		}

		@Override
		public void get(NbtElement current, List<NbtElement> results) {
			if (current instanceof AbstractNbtList<?> abstractNbtList) {
				results.addAll(abstractNbtList);
			}
		}

		@Override
		public void getOrInit(NbtElement current, Supplier<NbtElement> source, List<NbtElement> results) {
			if (current instanceof AbstractNbtList<?> abstractNbtList) {
				if (abstractNbtList.isEmpty()) {
					NbtElement nbtElement = source.get();
					if (abstractNbtList.addElement(0, nbtElement)) {
						results.add(nbtElement);
					}
				} else {
					results.addAll(abstractNbtList);
				}
			}
		}

		@Override
		public NbtElement init() {
			return new NbtList();
		}

		@Override
		public int set(NbtElement current, Supplier<NbtElement> source) {
			if (!(current instanceof AbstractNbtList<?> abstractNbtList)) {
				return 0;
			}
            int i = abstractNbtList.size();
			if (i == 0) {
				abstractNbtList.addElement(0, source.get());
				return 1;
			}
			NbtElement nbtElement = source.get();
			int j = i - (int) abstractNbtList.stream().filter(nbtElement::equals).count();
			if (j == 0) {
				return 0;
			}
			abstractNbtList.clear();
			if (!abstractNbtList.addElement(0, nbtElement)) {
				return 0;
			}
			for (int k = 1; k < i; ++k) {
				abstractNbtList.addElement(k, source.get());
			}

			return j;
		}

		@Override
		public int clear(NbtElement current) {
			if (current instanceof AbstractNbtList<?> abstractNbtList) {
				int i = abstractNbtList.size();
				if (i > 0) {
					abstractNbtList.clear();
					return i;
				}
			}

			return 0;
		}
	}

	static class FilteredListElementNode implements PathNode {
		private final NbtCompound filter;
		private final Predicate<NbtElement> predicate;

		public FilteredListElementNode(NbtCompound filter) {
			this.filter = filter;
			this.predicate = CNbtPathArgumentType.getPredicate(filter);
		}

		@Override
		public void get(NbtElement current, List<NbtElement> results) {
			if (current instanceof NbtList nbtList) {
				nbtList.stream().filter(this.predicate).forEach(results::add);
			}
		}

		@Override
		public void getOrInit(NbtElement current, Supplier<NbtElement> source, List<NbtElement> results) {
			MutableBoolean mutableBoolean = new MutableBoolean();
			if (current instanceof NbtList nbtList) {
				nbtList.stream().filter(this.predicate).forEach(nbt -> {
					results.add(nbt);
					mutableBoolean.setTrue();
				});
				if (mutableBoolean.isFalse()) {
					NbtCompound nbtCompound = this.filter.copy();
					nbtList.add(nbtCompound);
					results.add(nbtCompound);
				}
			}
		}

		@Override
		public NbtElement init() {
			return new NbtList();
		}

		@Override
		public int set(NbtElement current, Supplier<NbtElement> source) {
			int i = 0;
			if (current instanceof NbtList nbtList) {
				int j = nbtList.size();
				if (j == 0) {
					nbtList.add(source.get());
					++i;
				} else {
					for (int k = 0; k < j; ++k) {
						NbtElement nbtElement = nbtList.get(k);
						if (this.predicate.test(nbtElement)) {
							NbtElement nbtElement2 = source.get();
							if (!nbtElement2.equals(nbtElement) && nbtList.setElement(k, nbtElement2)) {
								++i;
							}
						}
					}
				}
			}

			return i;
		}

		@Override
		public int clear(NbtElement current) {
			int i = 0;
			if (current instanceof NbtList nbtList) {
				for (int j = nbtList.size() - 1; j >= 0; --j) {
					if (this.predicate.test(nbtList.get(j))) {
						nbtList.remove(j);
						++i;
					}
				}
			}

			return i;
		}
	}

	static class FilteredNamedNode implements PathNode {
		private final String name;
		private final NbtCompound filter;
		private final Predicate<NbtElement> predicate;

		public FilteredNamedNode(String name, NbtCompound filter) {
			this.name = name;
			this.filter = filter;
			this.predicate = CNbtPathArgumentType.getPredicate(filter);
		}

		@Override
		public void get(NbtElement current, List<NbtElement> results) {
			if (current instanceof NbtCompound) {
				NbtElement nbtElement = ((NbtCompound)current).get(this.name);
				if (this.predicate.test(nbtElement)) {
					results.add(nbtElement);
				}
			}
		}

		@Override
		public void getOrInit(NbtElement current, Supplier<NbtElement> source, List<NbtElement> results) {
			if (current instanceof NbtCompound nbtCompound) {
				NbtElement nbtElement = nbtCompound.get(this.name);
				if (nbtElement == null) {
					NbtElement var6 = this.filter.copy();
					nbtCompound.put(this.name, var6);
					results.add(var6);
				} else if (this.predicate.test(nbtElement)) {
					results.add(nbtElement);
				}
			}
		}

		@Override
		public NbtElement init() {
			return new NbtCompound();
		}

		@Override
		public int set(NbtElement current, Supplier<NbtElement> source) {
			if (current instanceof NbtCompound nbtCompound) {
				NbtElement nbtElement = nbtCompound.get(this.name);
				if (this.predicate.test(nbtElement)) {
					NbtElement nbtElement2 = source.get();
					if (!nbtElement2.equals(nbtElement)) {
						nbtCompound.put(this.name, nbtElement2);
						return 1;
					}
				}
			}

			return 0;
		}

		@Override
		public int clear(NbtElement current) {
			if (current instanceof NbtCompound nbtCompound) {
				NbtElement nbtElement = nbtCompound.get(this.name);
				if (this.predicate.test(nbtElement)) {
					nbtCompound.remove(this.name);
					return 1;
				}
			}

			return 0;
		}
	}

	static class FilteredRootNode implements PathNode {
		private final Predicate<NbtElement> matcher;

		public FilteredRootNode(NbtCompound filter) {
			this.matcher = CNbtPathArgumentType.getPredicate(filter);
		}

		@Override
		public void get(NbtElement current, List<NbtElement> results) {
			if (current instanceof NbtCompound && this.matcher.test(current)) {
				results.add(current);
			}
		}

		@Override
		public void getOrInit(NbtElement current, Supplier<NbtElement> source, List<NbtElement> results) {
			this.get(current, results);
		}

		@Override
		public NbtElement init() {
			return new NbtCompound();
		}

		@Override
		public int set(NbtElement current, Supplier<NbtElement> source) {
			return 0;
		}

		@Override
		public int clear(NbtElement current) {
			return 0;
		}
	}

	static class IndexedListElementNode implements PathNode {
		private final int index;

		public IndexedListElementNode(int index) {
			this.index = index;
		}

		@Override
		public void get(NbtElement current, List<NbtElement> results) {
			if (current instanceof AbstractNbtList<?> abstractNbtList) {
				int i = abstractNbtList.size();
				int j = this.index < 0 ? i + this.index : this.index;
				if (0 <= j && j < i) {
					results.add(abstractNbtList.get(j));
				}
			}
		}

		@Override
		public void getOrInit(NbtElement current, Supplier<NbtElement> source, List<NbtElement> results) {
			this.get(current, results);
		}

		@Override
		public NbtElement init() {
			return new NbtList();
		}

		@Override
		public int set(NbtElement current, Supplier<NbtElement> source) {
			if (current instanceof AbstractNbtList<?> abstractNbtList) {
				int i = abstractNbtList.size();
				int j = this.index < 0 ? i + this.index : this.index;
				if (0 <= j && j < i) {
					NbtElement nbtElement = abstractNbtList.get(j);
					NbtElement nbtElement2 = source.get();
					if (!nbtElement2.equals(nbtElement) && abstractNbtList.setElement(j, nbtElement2)) {
						return 1;
					}
				}
			}

			return 0;
		}

		@Override
		public int clear(NbtElement current) {
			if (current instanceof AbstractNbtList<?> abstractNbtList) {
				int i = abstractNbtList.size();
				int j = this.index < 0 ? i + this.index : this.index;
				if (0 <= j && j < i) {
					abstractNbtList.remove(j);
					return 1;
				}
			}

			return 0;
		}
	}

	static class NamedNode implements PathNode {
		private final String name;

		public NamedNode(String name) {
			this.name = name;
		}

		@Override
		public void get(NbtElement current, List<NbtElement> results) {
			if (current instanceof NbtCompound) {
				NbtElement nbtElement = ((NbtCompound)current).get(this.name);
				if (nbtElement != null) {
					results.add(nbtElement);
				}
			}
		}

		@Override
		public void getOrInit(NbtElement current, Supplier<NbtElement> source, List<NbtElement> results) {
			if (current instanceof NbtCompound nbtCompound) {
				NbtElement nbtElement;
				if (nbtCompound.contains(this.name)) {
					nbtElement = nbtCompound.get(this.name);
				} else {
					nbtElement = source.get();
					nbtCompound.put(this.name, nbtElement);
				}

				results.add(nbtElement);
			}
		}

		@Override
		public NbtElement init() {
			return new NbtCompound();
		}

		@Override
		public int set(NbtElement current, Supplier<NbtElement> source) {
			if (current instanceof NbtCompound nbtCompound) {
				NbtElement nbtElement = source.get();
				NbtElement nbtElement2 = nbtCompound.put(this.name, nbtElement);
				if (!nbtElement.equals(nbtElement2)) {
					return 1;
				}
			}

			return 0;
		}

		@Override
		public int clear(NbtElement current) {
			if (current instanceof NbtCompound nbtCompound && nbtCompound.contains(this.name)) {
				nbtCompound.remove(this.name);
				return 1;
			}

			return 0;
		}
	}

	public static class NbtPath {
		private final String string;
		private final Object2IntMap<PathNode> nodeEndIndices;
		private final PathNode[] nodes;
		public static final Codec<NbtPath> CODEC = Codec.STRING.comapFlatMap(path -> {
			try {
				NbtPath nbtPath = new CNbtPathArgumentType().parse(new StringReader(path));
				return DataResult.success(nbtPath);
			} catch (CommandSyntaxException var2) {
				return DataResult.error(() -> "Failed to parse path " + path + ": " + var2.getMessage());
			}
		}, NbtPath::getString);

		public static NbtPath parse(String path) throws CommandSyntaxException {
			return new CNbtPathArgumentType().parse(new StringReader(path));
		}

		public NbtPath(String string, PathNode[] nodes, Object2IntMap<PathNode> nodeEndIndices) {
			this.string = string;
			this.nodes = nodes;
			this.nodeEndIndices = nodeEndIndices;
		}

		public List<NbtElement> get(NbtElement element) throws CommandSyntaxException {
			List<NbtElement> list = Collections.singletonList(element);

			for(PathNode pathNode : this.nodes) {
				list = pathNode.get(list);
				if (list.isEmpty()) {
					throw this.createNothingFoundException(pathNode);
				}
			}

			return list;
		}

		public int count(NbtElement element) {
			List<NbtElement> list = Collections.singletonList(element);

			for (PathNode pathNode : this.nodes) {
				list = pathNode.get(list);
				if (list.isEmpty()) {
					return 0;
				}
			}

			return list.size();
		}

		private List<NbtElement> getTerminals(NbtElement start) throws CommandSyntaxException {
			List<NbtElement> list = Collections.singletonList(start);

			for (int i = 0; i < this.nodes.length - 1; ++i) {
				PathNode pathNode = this.nodes[i];
				int j = i + 1;
				list = pathNode.getOrInit(list, this.nodes[j]::init);
				if (list.isEmpty()) {
					throw this.createNothingFoundException(pathNode);
				}
			}

			return list;
		}

		public List<NbtElement> getOrInit(NbtElement element, Supplier<NbtElement> source) throws CommandSyntaxException {
			List<NbtElement> list = this.getTerminals(element);
			PathNode pathNode = this.nodes[this.nodes.length - 1];
			return pathNode.getOrInit(list, source);
		}

		private static int forEach(List<NbtElement> elements, Function<NbtElement, Integer> operation) {
			return elements.stream().map(operation).reduce(0, Integer::sum);
		}

		public static boolean isTooDeep(NbtElement element, int depth) {
			if (depth >= 512) {
				return true;
			}
			if (element instanceof NbtCompound nbtCompound) {
				for (String string : nbtCompound.getKeys()) {
					NbtElement nbtElement = nbtCompound.get(string);
					if (nbtElement != null && isTooDeep(nbtElement, depth + 1)) {
						return true;
					}
				}
			} else if (element instanceof NbtList) {
				for (NbtElement nbtElement2 : (NbtList)element) {
					if (isTooDeep(nbtElement2, depth + 1)) {
						return true;
					}
				}
			}

			return false;
		}

		public int put(NbtElement element, NbtElement source) throws CommandSyntaxException {
			if (isTooDeep(source, this.getDepth())) {
				throw CNbtPathArgumentType.TOO_DEEP_EXCEPTION.create();
			}
			NbtElement nbtElement = source.copy();
			List<NbtElement> list = this.getTerminals(element);
			if (list.isEmpty()) {
				return 0;
			}
			PathNode pathNode = this.nodes[this.nodes.length - 1];
			MutableBoolean mutableBoolean = new MutableBoolean(false);
			return forEach(list, nbt -> pathNode.set(nbt, () -> {
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

		public int insert(int index, NbtCompound compound, List<NbtElement> elements) throws CommandSyntaxException {
			List<NbtElement> list = new ArrayList<>(elements.size());

			for (NbtElement nbtElement : elements) {
				NbtElement nbtElement2 = nbtElement.copy();
				list.add(nbtElement2);
				if (isTooDeep(nbtElement2, this.getDepth())) {
					throw CNbtPathArgumentType.TOO_DEEP_EXCEPTION.create();
				}
			}

			Collection<NbtElement> collection = this.getOrInit(compound, NbtList::new);
			int i = 0;
			boolean bl = false;

			for (NbtElement nbtElement3 : collection) {
				if (!(nbtElement3 instanceof AbstractNbtList<?> abstractNbtList)) {
					throw CNbtPathArgumentType.EXPECTED_LIST_EXCEPTION.create(nbtElement3);
				}

                boolean bl2 = false;
				int j = index < 0 ? abstractNbtList.size() + index + 1 : index;

				for (NbtElement nbtElement4 : list) {
					try {
						if (abstractNbtList.addElement(j, bl ? nbtElement4.copy() : nbtElement4)) {
							++j;
							bl2 = true;
						}
					} catch (IndexOutOfBoundsException var16) {
						throw CNbtPathArgumentType.INVALID_INDEX_EXCEPTION.create(j);
					}
				}

				bl = true;
				i += bl2 ? 1 : 0;
			}

			return i;
		}

		public int remove(NbtElement element) {
			List<NbtElement> list = Collections.singletonList(element);

			for (int i = 0; i < this.nodes.length - 1; ++i) {
				list = this.nodes[i].get(list);
			}

			PathNode pathNode = this.nodes[this.nodes.length - 1];
			return forEach(list, pathNode::clear);
		}

		private CommandSyntaxException createNothingFoundException(PathNode node) {
			int i = this.nodeEndIndices.getInt(node);
			return CNbtPathArgumentType.NOTHING_FOUND_EXCEPTION.create(this.string.substring(0, i));
		}

		public String toString() {
			return this.string;
		}

		public String getString() {
			return this.string;
		}
	}

	interface PathNode {
		void get(NbtElement current, List<NbtElement> results);

		void getOrInit(NbtElement current, Supplier<NbtElement> source, List<NbtElement> results);

		NbtElement init();

		int set(NbtElement current, Supplier<NbtElement> source);

		int clear(NbtElement current);

		default List<NbtElement> get(List<NbtElement> elements) {
			return this.process(elements, this::get);
		}

		default List<NbtElement> getOrInit(List<NbtElement> elements, Supplier<NbtElement> supplier) {
			return this.process(elements, (current, results) -> this.getOrInit(current, supplier, results));
		}

		default List<NbtElement> process(List<NbtElement> elements, BiConsumer<NbtElement, List<NbtElement>> action) {
			List<NbtElement> list = Lists.newArrayList();

			for(NbtElement nbtElement : elements) {
				action.accept(nbtElement, list);
			}

			return list;
		}
	}
}
