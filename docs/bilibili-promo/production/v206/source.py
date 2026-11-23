"""读取单段实机子序列的原始路径和时间映射，允许在没有试制缓存时重建素材。"""
import gzip
from pathlib import Path
import xml.etree.ElementTree as ET

TICKS = 254016000000


def sequence_ranges(project):
    with gzip.open(project, 'rb') as stream:
        root = ET.parse(stream).getroot()
    objects = {obj.get('ObjectID', obj.get('ObjectUID')): obj for obj in root}

    def resolve(link):
        return objects[link.get('ObjectRef', link.get('ObjectURef'))]

    ranges = {}
    for seq in root.findall('Sequence'):
        name = seq.findtext('Name').strip()
        items = []
        for link in seq.iter('Second'):
            if link.get('ObjectRef') in objects and resolve(link).tag == 'VideoTrackGroup':
                for track in resolve(link).findall('.//Tracks/Track'):
                    items.extend(resolve(track).findall('.//TrackItems/TrackItem'))
        if len(items) != 1:
            raise ValueError(f'{name} 的视频结构已改变，请先导出当前命名序列。')
        item = resolve(items[0]).find('ClipTrackItem')
        timing = item.find('TrackItem')
        clip = resolve(resolve(item.find('SubClip')).find('Clip')).find('Clip')
        media = resolve(resolve(clip.find('Source')).find('.//Media'))
        offset = int(clip.findtext('InPoint', '0')) - int(timing.findtext('Start', '0'))
        ranges[name] = (Path(media.findtext('FilePath')), offset / TICKS, int(timing.findtext('End')) / TICKS)
    return ranges
