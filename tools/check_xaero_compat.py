#!/usr/bin/env python3
"""静态核对打包产物对真实 Xaero JAR 的链接、回调与反射契约；不启动游戏。

默认检查 gradle.properties 的全部目标及 .deps/compat-new-<MC> 中已下载的新依赖。
用 --versions 1.20.1 1.21.1 可缩小范围。依赖来自本地 Gradle 缓存，不自动联网。
"""

import argparse
import io
import json
from pathlib import Path
import re
import struct
import sys
import zipfile


class JavaClass:
    def __init__(self, data):
        stream = io.BytesIO(data)

        def number(fmt):
            return struct.unpack(fmt, stream.read(struct.calcsize(fmt)))[0]

        u1 = lambda: number('>B')
        u2 = lambda: number('>H')
        u4 = lambda: number('>I')
        if u4() != 0xCAFEBABE:
            raise ValueError('无效 class 文件')
        stream.read(4)
        pool = [None] * u2()
        index = 1
        while index < len(pool):
            tag = u1()
            if tag == 1:
                pool[index] = (tag, stream.read(u2()).decode('utf-8', 'replace'))
            elif tag in (3, 4):
                stream.read(4)
            elif tag in (5, 6):
                stream.read(8)
                index += 1
            elif tag in (7, 8, 16, 19, 20):
                pool[index] = (tag, u2())
            elif tag in (9, 10, 11, 12, 17, 18):
                pool[index] = (tag, u2(), u2())
            elif tag == 15:
                pool[index] = (tag, u1(), u2())
            else:
                raise ValueError(f'未知常量池标记 {tag}')
            index += 1
        utf = lambda i: pool[i][1]
        cls = lambda i: utf(pool[i][1]) if i else None
        self.class_references = [cls(i) for i, entry in enumerate(pool) if entry and entry[0] == 7]
        self.access = u2()
        self.name = cls(u2())
        self.parent = cls(u2())
        self.interfaces = [cls(u2()) for _ in range(u2())]
        self.fields, self.methods = {}, {}
        for members in (self.fields, self.methods):
            for _ in range(u2()):
                flags, name, descriptor = u2(), utf(u2()), utf(u2())
                members[(name, descriptor)] = flags
                for _ in range(u2()):
                    u2()
                    stream.read(u4())
        self.references = []
        for entry in pool:
            if entry and entry[0] in (9, 10, 11):
                signature = pool[entry[2]]
                self.references.append((entry[0], cls(entry[1]), utf(signature[1]), utf(signature[2])))


def classes_from_jar(path, prefix=''):
    classes = {}

    def read(archive):
        for name in archive.namelist():
            if name.endswith('.class') and name.startswith(prefix):
                cls = JavaClass(archive.read(name))
                classes[cls.name] = cls
            elif not prefix and name.startswith('META-INF/jars/') and name.endswith('.jar'):
                with zipfile.ZipFile(io.BytesIO(archive.read(name))) as nested:
                    read(nested)

    with zipfile.ZipFile(path) as archive:
        read(archive)
    return classes


def lookup(classes, owner, member, field=False, visited=None):
    # Minecraft 继承链由该目标的构建验证；这里不把未加载游戏类误报为 Xaero 缺失符号。
    if owner and owner.startswith(('net/minecraft/', 'com/mojang/')):
        return None
    if owner not in classes:
        return False
    visited = set() if visited is None else visited
    if owner in visited:
        return False
    visited.add(owner)
    cls = classes[owner]
    if member in (cls.fields if field else cls.methods):
        return True
    if member[0] == '<init>':
        return False
    results = [lookup(classes, parent, member, field, visited)
               for parent in (cls.parent, *cls.interfaces) if parent]
    return True if True in results else None if None in results else False


def ancestors(classes, cls):
    seen = set()
    while cls.parent in classes and cls.parent not in seen:
        seen.add(cls.parent)
        cls = classes[cls.parent]
        yield cls


def inspect(artifact, dependencies):
    mod = classes_from_jar(artifact, 'areahint/')
    with zipfile.ZipFile(artifact) as archive:
        mixins = json.loads(archive.read('areas-hint.client.mixins.json'))
    tooltip_fallback = ('XaeroWorldMapTooltipMixin' in mixins.get('client', [])
                        and 'areahint/mixin/client/XaeroWorldMapTooltipMixin' in mod
                        and 'areahint/xaero/XaeroTooltipFallback' in mod)
    external = {}
    for path in dependencies:
        external.update(classes_from_jar(path))
    classes = {**external, **mod}
    problems, mitigated = set(), set()
    references = set()
    for cls in mod.values():
        for kind, owner, name, descriptor in cls.references:
            if not owner.startswith('xaero/'):
                continue
            references.add((owner, name, descriptor))
            if lookup(classes, owner, (name, descriptor), kind == 9) is False:
                problems.add(f'缺失链接 {owner}.{name}{descriptor}')
        if not cls.name.startswith('areahint/xaero/') or cls.access & 0x0400:
            continue
        hierarchy = [cls, *ancestors(classes, cls)]
        for parent in hierarchy[1:]:
            if not parent.name.startswith('xaero/'):
                continue
            for member, access in parent.methods.items():
                if access & 0x0400:
                    implementation = next((c.methods[member] for c in hierarchy if member in c.methods), 0x0400)
                    if implementation & 0x0400:
                        problems.add(f'未实现抽象回调 {cls.name}.{member[0]}{member[1]}')
                name, descriptor = member
                for own_name, own_descriptor in cls.methods:
                    if (name != own_name or name.startswith('<') or descriptor == own_descriptor
                            or descriptor.split(')')[0] != own_descriptor.split(')')[0]
                            or member in cls.methods):
                        continue
                    issue = f'回调返回类型变化 {cls.name}.{name}: {own_descriptor} -> {descriptor}'
                    # 专用 Mixin 将旧 CursorBox 回调接回 Xaero 原生提示流程；只接受这一种已知适配。
                    if (name == 'getTooltip'
                            and tooltip_fallback
                            and own_descriptor.endswith('Lxaero/map/gui/CursorBox;')
                            and descriptor.endswith('Lxaero/lib/client/gui/widget/Tooltip;')):
                        mitigated.add(issue)
                    else:
                        problems.add(issue)

    required_fields = {
        'xaero/hud/minimap/element/render/over/MinimapElementOverMapRendererHandler':
            {'ps': 'D', 'pc': 'D', 'zoom': 'D', 'halfViewW': 'I', 'halfViewH': 'I', 'circle': 'Z'},
        'xaero/map/gui/GuiMap': {'cameraX': 'D', 'cameraZ': 'D', 'scale': 'D', 'screenScale': 'D',
                               'shouldResetCameraPos': 'Z'},
    }
    for owner, fields in required_fields.items():
        cls = external.get(owner)
        for name, descriptor in fields.items():
            if cls is None or (name, descriptor) not in cls.fields:
                problems.add(f'缺失反射字段 {owner}.{name}:{descriptor}')
    gui = external.get('xaero/map/gui/GuiMap')
    for name in ('lastViewedDimensionId', 'lastNonNullViewedDimensionId', 'cameraDestination',
                 'cameraDestinationAnimX', 'cameraDestinationAnimZ'):
        if gui is None or not any(field[0] == name for field in gui.fields):
            problems.add(f'缺失反射字段 GuiMap.{name}')
    for member in (('getCurrentMapCoordinateScale', '()D'), ('changeZoom', '(DI)V')):
        if gui is None or member not in gui.methods:
            problems.add(f'缺失反射方法 GuiMap.{member}')
    if mitigated and not any(name == 'hoveredElementTooltipHelper'
                             and descriptor == '(Lxaero/map/element/HoveredMapElementHolder;Z)Lxaero/lib/client/gui/widget/Tooltip;'
                             for name, descriptor in gui.methods):
        problems.add('缺失提示回退的 Mixin 注入目标')
    renderer = mod.get('areahint/xaero/worldmap/AreaWorldMapRenderer')
    handler = external.get('xaero/map/element/MapElementRenderHandler')
    if renderer is not None and handler is not None:
        accepted_types = {cls.name for cls in [renderer, *ancestors(classes, renderer)]}
        if not any(name == 'add' and descriptor.startswith('(L') and descriptor.endswith(';)V')
                   and descriptor[2:-3] in accepted_types for name, descriptor in handler.methods):
            problems.add('世界地图注册器不存在可接收域名渲染器的 add 方法')
    return len(references), sorted(problems), sorted(mitigated)


def main():
    # Windows 的终端默认代码页可能不是 UTF-8，保持审计输出与仓库中文编码一致。
    if hasattr(sys.stdout, 'reconfigure'):
        sys.stdout.reconfigure(encoding='utf-8')
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument('--versions', nargs='+')
    args = parser.parse_args()
    root = args.root
    props = dict(re.findall(r'^([^#\s=]+)=(.*)$', (root / 'gradle.properties').read_text(encoding='utf-8'), re.M))
    versions = args.versions or [key[5:-10] for key in props if key.startswith('deps.') and key.endswith('.minecraft')]
    failures = 0
    checks = 0
    for version in versions:
        project = root / ('modern/versions' if version.startswith('26.') else 'versions') / version
        artifact = project / 'build/libs' / f"areas-hint-fabric-{version}-{props['mod_version']}.jar"
        try:
            configured = [f'deps.{version}.{name}' in props for name in ('xaero_minimap', 'xaero_world_map')]
            if not any(configured):
                # 上游没有对应版本时仍检查真实成品，确保核心代码不残留第三方硬链接。
                compiled = classes_from_jar(artifact, prefix='areahint/')
                if not compiled:
                    raise ValueError('成品没有域名实现类，不能作为无依赖目标通过检查')
                problems = []
                for cls in compiled.values():
                    # 类常量覆盖继承、类型转换和类字面量，描述符另覆盖未在方法体中使用的字段与参数。
                    referenced_types = set(cls.class_references)
                    descriptors = [descriptor for _, descriptor in list(cls.fields) + list(cls.methods)]
                    descriptors.extend(reference[3] for reference in cls.references)
                    for descriptor in descriptors:
                        referenced_types.update(re.findall(r'L([^;]+);', descriptor))
                    for reference in referenced_types:
                        name = reference.lstrip('[')
                        if name.startswith('L') and name.endswith(';'):
                            name = name[1:-1]
                        if name.startswith(('xaero/', 'xaerolib/')):
                            problems.append((cls.name, name))
                checks += 1
                failures += bool(problems)
                print(f'{version} without Xaero: {len(problems)} hard dependency references')
                for issue in problems:
                    print(f'  FAIL {issue}')
                continue
            baseline = []
            for name, slug in (('xaero_minimap', 'xaeros-minimap'), ('xaero_world_map', 'xaeros-world-map')):
                dep = props[f'deps.{version}.{name}']
                folder = root / '.gradle-home/caches/modules-2/files-2.1/maven.modrinth' / slug / dep
                matches = list(folder.glob('*/*.jar'))
                baseline.append(next(p for p in matches if not p.name.endswith(('-sources.jar', '-javadoc.jar'))))
            candidates = list((root / f'.deps/compat-new-{version}').glob('xaero*map-*.jar'))
            runs = [('baseline', baseline)]
            if len(candidates) == 2:
                runs.append(('updated', candidates))
            for label, dependencies in runs:
                count, problems, mitigated = inspect(artifact, dependencies)
                checks += 1
                failures += bool(problems)
                print(f'{version} {label}: {count} references, {len(problems)} issues, {len(mitigated)} adapted callbacks')
                for issue in problems:
                    print(f'  FAIL {issue}')
        except (OSError, ValueError, StopIteration, KeyError) as error:
            failures += 1
            print(f'{version}: missing/invalid input: {error}')
    print(f'{checks} dependency combinations checked; {failures} failures. Static checks do not replace in-game verification.')
    return 1 if failures else 0


if __name__ == '__main__':
    sys.exit(main())
