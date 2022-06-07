package dev.xpple.clientarguments.arguments;

import com.google.common.collect.Lists;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
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
import java.util.stream.Stream;

public class CNbtPathArgumentType implements ArgumentType<CNbtPathArgumentType.NbtPath> {

	private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo.bar", "foo[0]", "[0]", "[]", "{foo=bar}");
	public static final SimpleCommandExceptionType INVALID_PATH_NODE_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("carguments.nbtpath.node.invalid"));
	public static final DynamicCommandExceptionType NOTHING_FOUND_EXCEPTION = new DynamicCommandExceptionType(path -> Text.translatable("carguments.nbtpath.nothing_found", path));

	public static CNbtPathArgumentType nbtPath() {
		return new CNbtPathArgumentType();
	}

	public static NbtPath getCNbtPath(final CommandContext<FabricClientCommandSource> context, final String name) {
		return context.getArgument(name, NbtPath.class);
	}

	@Override
	public NbtPath parse(final StringReader stringReader) throws CommandSyntaxException {
		List<PathNode> list = Lists.newArrayList();
		int cursor = stringReader.getCursor();
		Object2IntMap<PathNode> object2IntMap = new Object2IntOpenHashMap<>();
		boolean bl = true;

		while(stringReader.canRead() && stringReader.peek() != ' ') {
			PathNode pathNode = parseNode(stringReader, bl);
			list.add(pathNode);
			object2IntMap.put(pathNode, stringReader.getCursor() - cursor);
			bl = false;
			if (stringReader.canRead()) {
				char c = stringReader.peek();
				if (c != ' ' && c != '[' && c != '{') {
					stringReader.expect('.');
				}
			}
		}

		return new NbtPath(stringReader.getString().substring(cursor, stringReader.getCursor()), list.toArray(new PathNode[0]), object2IntMap);
	}

	private static PathNode parseNode(StringReader reader, boolean root) throws CommandSyntaxException {
		switch (reader.peek()) {
			case '"' -> {
				return readCompoundChildNode(reader, reader.readString());
			}
			case '[' -> {
				reader.skip();
				int peek = reader.peek();
				if (peek == '{') {
					NbtCompound nbtCompound2 = (new StringNbtReader(reader)).parseCompound();
					reader.expect(']');
					return new FilteredListElementNode(nbtCompound2);
				} else {
					if (peek == ']') {
						reader.skip();
						return AllListElementNode.INSTANCE;
					}

					int j = reader.readInt();
					reader.expect(']');
					return new IndexedListElementNode(j);
				}
			}
			case '{' -> {
				if (!root) {
					throw INVALID_PATH_NODE_EXCEPTION.createWithContext(reader);
				}
				NbtCompound nbtCompound = (new StringNbtReader(reader)).parseCompound();
				return new FilteredRootNode(nbtCompound);
			}
			default -> {
				return readCompoundChildNode(reader, readName(reader));
			}
		}
	}

	private static PathNode readCompoundChildNode(StringReader reader, String name) throws CommandSyntaxException {
		if (reader.canRead() && reader.peek() == '{') {
			NbtCompound nbtCompound = (new StringNbtReader(reader)).parseCompound();
			return new FilteredNamedNode(name, nbtCompound);
		}
		return new NamedNode(name);
	}

	private static String readName(StringReader reader) throws CommandSyntaxException {
		int cursor = reader.getCursor();

		while(reader.canRead() && isNameCharacter(reader.peek())) {
			reader.skip();
		}

		if (reader.getCursor() == cursor) {
			throw INVALID_PATH_NODE_EXCEPTION.createWithContext(reader);
		}
		return reader.getString().substring(cursor, reader.getCursor());
	}

	public Collection<String> getExamples() {
		return EXAMPLES;
	}

	private static boolean isNameCharacter(char c) {
		return c != ' ' && c != '"' && c != '[' && c != ']' && c != '.' && c != '{' && c != '}';
	}

	static Predicate<NbtElement> getPredicate(NbtCompound filter) {
		return nbtElement -> NbtHelper.matches(filter, nbtElement, true);
	}

	public static class NbtPath {
		private final String string;
		private final Object2IntMap<PathNode> nodeEndIndices;
		private final PathNode[] nodes;

		public NbtPath(String string, PathNode[] nodes, Object2IntMap<PathNode> nodeEndIndices) {
			this.string = string;
			this.nodes = nodes;
			this.nodeEndIndices = nodeEndIndices;
		}

		public List<NbtElement> get(NbtElement element) throws CommandSyntaxException {
			List<NbtElement> list = Collections.singletonList(element);

			for (PathNode pathNode : this.nodes) {
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

			for(int i = 0; i < this.nodes.length - 1; ++i) {
				PathNode pathNode = this.nodes[i];
				int j = i + 1;
				PathNode nextPathNode = this.nodes[j];
				Objects.requireNonNull(nextPathNode);
				list = pathNode.getOrInit(list, nextPathNode::init);
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

		public int put(NbtElement element, NbtElement source) throws CommandSyntaxException {
			Objects.requireNonNull(source);
			return this.put(element, source::copy);
		}

		public int put(NbtElement element, Supplier<NbtElement> source) throws CommandSyntaxException {
			List<NbtElement> list = this.getTerminals(element);
			PathNode pathNode = this.nodes[this.nodes.length - 1];
			return forEach(list, (nbt) -> pathNode.set(nbt, source));
		}

		public int remove(NbtElement element) {
			List<NbtElement> list = Collections.singletonList(element);

			for(int i = 0; i < this.nodes.length - 1; ++i) {
				list = this.nodes[i].get(list);
			}

			PathNode pathNode = this.nodes[this.nodes.length - 1];
			Objects.requireNonNull(pathNode);
			return forEach(list, pathNode::clear);
		}

		private CommandSyntaxException createNothingFoundException(PathNode node) {
			int i = this.nodeEndIndices.getInt(node);
			return CNbtPathArgumentType.NOTHING_FOUND_EXCEPTION.create(this.string.substring(0, i));
		}

		public String toString() {
			return this.string;
		}
	}

	private interface PathNode {
		void get(NbtElement current, List<NbtElement> results);

		void getOrInit(NbtElement current, Supplier<NbtElement> source, List<NbtElement> results);

		NbtElement init();

		int set(NbtElement current, Supplier<NbtElement> source);

		int clear(NbtElement current);

		default List<NbtElement> get(List<NbtElement> elements) {
			return this.process(elements, this::get);
		}

		default List<NbtElement> getOrInit(List<NbtElement> elements, Supplier<NbtElement> supplier) {
			return this.process(elements, (current, results) -> {
				this.getOrInit(current, supplier, results);
			});
		}

		default List<NbtElement> process(List<NbtElement> elements, BiConsumer<NbtElement, List<NbtElement>> action) {
			List<NbtElement> list = Lists.newArrayList();

			for (NbtElement nbtElement : elements) {
				action.accept(nbtElement, list);
			}

			return list;
		}
	}

	static class FilteredRootNode implements PathNode {
		private final Predicate<NbtElement> matcher;

		public FilteredRootNode(NbtCompound filter) {
			this.matcher = CNbtPathArgumentType.getPredicate(filter);
		}

		public void get(NbtElement current, List<NbtElement> results) {
			if (current instanceof NbtCompound && this.matcher.test(current)) {
				results.add(current);
			}

		}

		public void getOrInit(NbtElement current, Supplier<NbtElement> source, List<NbtElement> results) {
			this.get(current, results);
		}

		public NbtElement init() {
			return new NbtCompound();
		}

		public int set(NbtElement current, Supplier<NbtElement> source) {
			return 0;
		}

		public int clear(NbtElement current) {
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

		public void get(NbtElement current, List<NbtElement> results) {
			if (current instanceof NbtList nbtList) {
				Stream<NbtElement> elementStream = nbtList.stream().filter(this.predicate);
				Objects.requireNonNull(results);
				elementStream.forEach(results::add);
			}

		}

		public void getOrInit(NbtElement current, Supplier<NbtElement> source, List<NbtElement> results) {
			MutableBoolean mutableBoolean = new MutableBoolean();
			if (current instanceof NbtList nbtList) {
				nbtList.stream().filter(this.predicate).forEach((nbt) -> {
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

		public NbtElement init() {
			return new NbtList();
		}

		public int set(NbtElement current, Supplier<NbtElement> source) {
			int i = 0;
			if (current instanceof NbtList nbtList) {
				int j = nbtList.size();
				if (j == 0) {
					nbtList.add(source.get());
					++i;
				} else {
					for(int k = 0; k < j; ++k) {
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

		public int clear(NbtElement current) {
			int i = 0;
			if (current instanceof NbtList nbtList) {
				for(int j = nbtList.size() - 1; j >= 0; --j) {
					if (this.predicate.test(nbtList.get(j))) {
						nbtList.remove(j);
						++i;
					}
				}
			}

			return i;
		}
	}

	static class AllListElementNode implements PathNode {
		public static final AllListElementNode INSTANCE = new AllListElementNode();

		private AllListElementNode() {
		}

		public void get(NbtElement current, List<NbtElement> results) {
			if (current instanceof AbstractNbtList abstractNbtList) {
				results.add(abstractNbtList);
			}
		}

		public void getOrInit(NbtElement current, Supplier<NbtElement> source, List<NbtElement> results) {
			if (current instanceof AbstractNbtList abstractNbtList) {
				if (abstractNbtList.isEmpty()) {
					NbtElement nbtElement = source.get();
					if (abstractNbtList.addElement(0, nbtElement)) {
						results.add(nbtElement);
					}
				} else {
					results.add(abstractNbtList);
				}
			}

		}

		public NbtElement init() {
			return new NbtList();
		}

		public int set(NbtElement current, Supplier<NbtElement> source) {
			if (!(current instanceof AbstractNbtList<?> abstractNbtList)) {
				return 0;
			} else {
				int size = abstractNbtList.size();
				if (size == 0) {
					abstractNbtList.addElement(0, source.get());
					return 1;
				} else {
					NbtElement nbtElement = source.get();
					Stream<?> var10001 = abstractNbtList.stream();
					Objects.requireNonNull(nbtElement);
					int j = size - (int) var10001.filter(nbtElement::equals).count();
					if (j == 0) {
						return 0;
					} else {
						abstractNbtList.clear();
						if (!abstractNbtList.addElement(0, nbtElement)) {
							return 0;
						} else {
							for(int k = 1; k < size; ++k) {
								abstractNbtList.addElement(k, source.get());
							}

							return j;
						}
					}
				}
			}
		}

		public int clear(NbtElement current) {
			if (current instanceof AbstractNbtList<?> abstractNbtList) {
				int size = abstractNbtList.size();
				if (size > 0) {
					abstractNbtList.clear();
					return size;
				}
			}

			return 0;
		}
	}

	static class IndexedListElementNode implements PathNode {
		private final int index;

		public IndexedListElementNode(int index) {
			this.index = index;
		}

		public void get(NbtElement current, List<NbtElement> results) {
			if (current instanceof AbstractNbtList<?> abstractNbtList) {
				int size = abstractNbtList.size();
				int j = this.index < 0 ? size + this.index : this.index;
				if (0 <= j && j < size) {
					results.add(abstractNbtList.get(j));
				}
			}

		}

		public void getOrInit(NbtElement current, Supplier<NbtElement> source, List<NbtElement> results) {
			this.get(current, results);
		}

		public NbtElement init() {
			return new NbtList();
		}

		public int set(NbtElement current, Supplier<NbtElement> source) {
			if (current instanceof AbstractNbtList<?> abstractNbtList) {
				int size = abstractNbtList.size();
				int j = this.index < 0 ? size + this.index : this.index;
				if (0 <= j && j < size) {
					NbtElement nbtElement = abstractNbtList.get(j);
					NbtElement nbtElement2 = source.get();
					if (!nbtElement2.equals(nbtElement) && abstractNbtList.setElement(j, nbtElement2)) {
						return 1;
					}
				}
			}

			return 0;
		}

		public int clear(NbtElement current) {
			if (current instanceof AbstractNbtList<?> abstractNbtList) {
				int size = abstractNbtList.size();
				int j = this.index < 0 ? size + this.index : this.index;
				if (0 <= j && j < size) {
					abstractNbtList.remove(j);
					return 1;
				}
			}

			return 0;
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

		public void get(NbtElement current, List<NbtElement> results) {
			if (current instanceof NbtCompound) {
				NbtElement nbtElement = ((NbtCompound)current).get(this.name);
				if (this.predicate.test(nbtElement)) {
					results.add(nbtElement);
				}
			}

		}

		public void getOrInit(NbtElement current, Supplier<NbtElement> source, List<NbtElement> results) {
			if (current instanceof NbtCompound nbtCompound) {
				NbtElement nbtElement = nbtCompound.get(this.name);
				if (nbtElement == null) {
					nbtElement = this.filter.copy();
					nbtCompound.put(this.name, nbtElement);
					results.add(nbtElement);
				} else if (this.predicate.test(nbtElement)) {
					results.add(nbtElement);
				}
			}

		}

		public NbtElement init() {
			return new NbtCompound();
		}

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

	private static class NamedNode implements PathNode {
		private final String name;

		public NamedNode(String name) {
			this.name = name;
		}

		public void get(NbtElement current, List<NbtElement> results) {
			if (current instanceof NbtCompound) {
				NbtElement nbtElement = ((NbtCompound)current).get(this.name);
				if (nbtElement != null) {
					results.add(nbtElement);
				}
			}
		}

		public void getOrInit(NbtElement current, Supplier<NbtElement> source, List<NbtElement> results) {
			if (current instanceof NbtCompound nbtCompound) {
				NbtElement nbtElement2;
				if (nbtCompound.contains(this.name)) {
					nbtElement2 = nbtCompound.get(this.name);
				} else {
					nbtElement2 = source.get();
					nbtCompound.put(this.name, nbtElement2);
				}

				results.add(nbtElement2);
			}
		}

		public NbtElement init() {
			return new NbtCompound();
		}

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

		public int clear(NbtElement current) {
			if (current instanceof NbtCompound nbtCompound) {
				if (nbtCompound.contains(this.name)) {
					nbtCompound.remove(this.name);
					return 1;
				}
			}
			return 0;
		}
	}
}
