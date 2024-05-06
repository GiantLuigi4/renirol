package tfc.renirol.frontend.rendering.selectors;

import org.lwjgl.vulkan.*;
import tfc.renirol.backend.vk.util.VkUtil;
import tfc.renirol.frontend.hardware.device.support.image.ReniSwapchainCapabilities;

import java.util.HashMap;

public class FormatSelector {
    ChannelInfo[] channels;

    public FormatSelector channels(ChannelInfo... channels) {
        this.channels = channels;
        return this;
    }

    String type;

    public FormatSelector type(String type) {
        this.type = type;
        return this;
    }

    public int select(int[] formats) {
        return VkUtil.select(
                formats,
                (format) -> {
                    int score = 0;

                    // TODO: temp
                    if (format == VK13.VK_FORMAT_R8G8B8A8_SRGB)
                        return 100000000;
                    else if (true) return 0;

                    String name = VkUtil.find(VK13.class, "VK_FORMAT_", format);

                    if (type == null || name.endsWith(type)) {
                        score += 250;
                    }

                    HashMap<Character, String> broken = new HashMap<>();
                    String build = "";
                    char channel = ' ';
                    name = name.substring("VK_FORMAT_".length());

                    for (char c : name.toCharArray()) {
                        if (c == '_') break;

                        if (channel != ' ' && !Character.isDigit(c)) {
                            broken.put(channel, build);
                            channel = ' ';
                            build = "";
                        }

                        if (channel == ' ')
                            channel = c;
                        else build += c;
                    }

                    if (channel != ' ') {
                        broken.put(channel, build);
                    }

                    for (ChannelInfo channelInfo : channels) {
                        if (!broken.containsKey(channelInfo.name)) {
                            score -= 1000;
                        }

                        boolean hasRequestedDepth = false;
                        String depth = broken.get(channelInfo.name);
                        int index = 0;
                        for (int bitDepth : channelInfo.bitDepths) {
                            if (String.valueOf(bitDepth).equals(depth)) {
                                score += (channelInfo.bitDepths.length - index) * 100;
                                hasRequestedDepth = true;
                                break;
                            }
                            index++;
                        }
                        if (!hasRequestedDepth) {
                            score -= 100 * Integer.parseInt(broken.get(channelInfo.name));
                        }
                    }

                    loopKeys:
                    for (Character c : broken.keySet()) {
                        for (ChannelInfo channelInfo : channels) {
                            if (channelInfo.name == c.charValue())
                                continue loopKeys;
                        }
                        score -= 1000 + (100 * Integer.parseInt(broken.get(c))); // extra information has overhead, and should be avoided if not requested
                    }

//                    System.out.println(name + " : " + score);
                    return score;
                }
        );
    }

    public VkSurfaceFormatKHR select(ReniSwapchainCapabilities image) {
//        loopForm:
//        for (VkSurfaceFormatKHR format : image.formats) {
//            String name = VkUtil.find(VK13.class, "VK_FORMAT_", format.format());
//
//            if (type == null || name.endsWith(type)) {
//                HashMap<Character, String> broken = new HashMap<>();
//                String build = "";
//                char channel = ' ';
//                name = name.substring("VK_FORMAT_".length());
//
//                for (char c : name.toCharArray()) {
//                    if (c == '_') break;
//
//                    if (channel != ' ' && !Character.isDigit(c)) {
//                        broken.put(channel, build);
//                        channel = ' ';
//                    }
//
//                    if (channel == ' ')
//                        channel = c;
//                    else build += c;
//                }
//
//                if (channel != ' ') {
//                    broken.put(channel, build);
//                }
//
//                loopChan:
//                for (ChannelInfo channelInfo : channels) {
//                    if (!broken.containsKey(channelInfo.name)) {
//                        continue loopForm;
//                    }
//                    String depth = broken.get(channelInfo.name);
//                    for (int bitDepth : channelInfo.bitDepths) {
//                        if (String.valueOf(bitDepth).equals(depth))
//                            continue loopChan;
//                    }
//                }
//
//                return format;
//            }
//        }
        return VkUtil.select(
                image.formats,
                (format) -> {
                    int score = 0;

                    String name = VkUtil.find(VK13.class, "VK_FORMAT_", format.format());

                    if (type == null || name.endsWith(type)) {
                        score += 250;
                    }

                    HashMap<Character, String> broken = new HashMap<>();
                    String build = "";
                    char channel = ' ';
                    name = name.substring("VK_FORMAT_".length());

                    for (char c : name.toCharArray()) {
                        if (c == '_') break;

                        if (channel != ' ' && !Character.isDigit(c)) {
                            broken.put(channel, build);
                            channel = ' ';
                            build = "";
                        }

                        if (channel == ' ')
                            channel = c;
                        else build += c;
                    }

                    if (channel != ' ') {
                        broken.put(channel, build);
                    }

                    for (ChannelInfo channelInfo : channels) {
                        if (!broken.containsKey(channelInfo.name)) {
                            score -= 1000;
                        }

                        boolean hasRequestedDepth = false;
                        String depth = broken.get(channelInfo.name);
                        int index = 0;
                        for (int bitDepth : channelInfo.bitDepths) {
                            if (String.valueOf(bitDepth).equals(depth)) {
                                score += (channelInfo.bitDepths.length - index) * 100;
                                hasRequestedDepth = true;
                                break;
                            }
                            index++;
                        }
                        if (!hasRequestedDepth) {
                            score -= 100 * Integer.parseInt(broken.get(channelInfo.name));
                        }
                    }

                    loopKeys:
                    for (Character c : broken.keySet()) {
                        for (ChannelInfo channelInfo : channels) {
                            if (channelInfo.name == c.charValue())
                                continue loopKeys;
                        }
                        score -= 1000 + (100 * Integer.parseInt(broken.get(c))); // extra information has overhead, and should be avoided if not requested
                    }

//                    System.out.println(name + " : " + score);
                    return score;
                }
        );
    }

    // TODO
}
