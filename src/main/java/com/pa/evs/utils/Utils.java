package com.pa.evs.utils;

import java.io.File;
import java.util.Random;

import org.apache.commons.lang3.StringUtils;

import com.pa.evs.dto.AddressDto;
import com.pa.evs.model.Address;
import com.pa.evs.model.CARequestLog;
import com.pa.evs.model.FloorLevel;


public class Utils {
    public static String formatMac(String mac) {
        char[] c = mac.toUpperCase().replace(":", "").toCharArray();
        mac = "";
        for (int i = 0; i < c.length; i++) {
            if (i % 2 == 0 && i > 0 && (i != c.length - 1)) {
                mac += ":";
            }
            mac += c[i];
        }

        return mac;
    }

    public static void mkdirs(String dir) {
        try {
            if (new File(dir).isDirectory()) {
                return;
            }
            String[] eles = dir.split("[\\\\/]");
            String tmp = "";
            for (int i = 0; i < eles.length; i++) {
                tmp += eles[i] + '/';
                File f = new File(tmp);
                if (!f.exists()) {
                    f.mkdir();
                }
            }
        } catch (Exception e) {
            //
        }

    }

    public static String formatHomeAddress(CARequestLog caRqlog) {
        StringBuilder address = new StringBuilder("");
        if (caRqlog.getBuilding() != null) {
        	Object streetNumber = caRqlog.getBuilding().getAddress().getStreetNumber();
        	if (streetNumber == null) {
        		streetNumber = "";
        	} else {
        		streetNumber = streetNumber + " ";
        	}
        	
        	if (caRqlog.getBlock() != null) {
        		address.append("Block ").append(caRqlog.getBlock().getName()).append(", ");
        	}
        	if (caRqlog.getBuildingUnit() != null && caRqlog.getFloorLevel() != null) {
        		String fName = caRqlog.getFloorLevel().getName();
        		if (fName == null) {
        			fName = caRqlog.getFloorLevel().getLevel();
        		}
        		String uName = caRqlog.getBuildingUnit().getName();
        		if (uName == null) {
        			uName = caRqlog.getBuildingUnit().getUnit();
        		}
        		address.append(fName).append("-").append(uName).append(" ");
        	} else if (caRqlog.getFloorLevel() != null) {
        		address.append(caRqlog.getFloorLevel().getName()).append(" ");
        	}
        	
            address.append(caRqlog.getBuilding().getName()).append(", ")
                    .append(streetNumber)
                    .append(StringUtils.isBlank(caRqlog.getBuilding().getAddress().getStreet()) ? "" : (caRqlog.getBuilding().getAddress().getStreet() + ", "))
                    .append(StringUtils.isBlank(caRqlog.getBuilding().getAddress().getTown()) ? "" : (caRqlog.getBuilding().getAddress().getTown() + ", "))
                    .append(StringUtils.isNotBlank(caRqlog.getBuilding().getAddress().getCity()) ? (caRqlog.getBuilding().getAddress().getCity() + ", ") : "")
                    .append(StringUtils.isNotBlank(caRqlog.getBuilding().getAddress().getCountry()) ? (caRqlog.getBuilding().getAddress().getCountry() + ", ") : "")
                    .append(caRqlog.getBuilding().getAddress().getPostalCode());
        } else if (caRqlog.getAddress() != null) {
        	return formatHomeAddress(null, caRqlog.getAddress());
        }
        return address.toString();
    }
    
    public static String formatHomeAddress(String buildingName, AddressDto addressDto) {
        StringBuilder address = new StringBuilder("");
        if (addressDto != null) {
        	Object streetNumber = addressDto.getStreetNumber();
        	if (streetNumber == null) {
        		streetNumber = "";
        	} else {
        		streetNumber = streetNumber + " ";
        	}
        	if (!StringUtils.isBlank(buildingName)) {
        		address.append(buildingName).append(", ");
        	}
        	address
                    .append(streetNumber)
                    .append(addressDto.getStreet()).append(", ")
                    .append(StringUtils.isBlank(addressDto.getTown()) ? "" : (addressDto.getTown() + ", "))
                    .append(StringUtils.isNotBlank(addressDto.getCity()) ? (addressDto.getCity() + ", ") : "").append(", ")
                    .append(addressDto.getCountry()).append(", ")
                    .append(addressDto.getPostalCode());
        }
        return address.toString();
    }
    
    public static String formatHomeAddress(String buildingName, Address addressE) {
        StringBuilder address = new StringBuilder("");
        if (addressE != null) {
        	Object streetNumber = addressE.getStreetNumber();
        	if (streetNumber == null) {
        		streetNumber = "";
        	} else {
        		streetNumber = streetNumber + " ";
        	}
        	if (!StringUtils.isBlank(buildingName)) {
        		address.append(buildingName).append(", ");
        	}
        	address
                    .append(streetNumber)
                    .append(StringUtils.isBlank(addressE.getStreet()) ? "" : (addressE.getStreet() + ", "))
                    .append(StringUtils.isBlank(addressE.getTown()) ? "" : (addressE.getTown() + ", "))
                    .append(addressE.getCity()).append(", ")
                    .append(addressE.getCountry()).append(", ")
                    .append(addressE.getPostalCode());
        }
        return address.toString();
    }

    public static String formatNetworkId(CARequestLog caRqlog) {
        StringBuilder networkId = new StringBuilder("");
        if (caRqlog.getBuilding() != null && caRqlog.getBuilding().getAddress() != null) {
            networkId.append(StringUtils.leftPad(caRqlog.getBuilding().getAddress().getPostalCode() + "", 7, '0'));
            networkId.append(StringUtils.leftPad(caRqlog.getBuilding().getAddress().getStreetNumber() + "", 5, '0'));
            networkId.append("0000000000");
        }
        if (caRqlog.getBuildingUnit() != null) {
            FloorLevel floorLevel = caRqlog.getBuildingUnit().getFloorLevel();
            if (floorLevel != null) {
                networkId.append(StringUtils.leftPad(floorLevel.getLevel() + "", 5, '0'));
            } else {
                networkId.append("00000");
            }
            networkId.append(StringUtils.leftPad(caRqlog.getBuildingUnit().getUnit() + "", 5, '0'));
        }
        return networkId.toString();
    }

	public static String buildAddressKey(CARequestLog caRqlog) {
		StringBuilder networkId = new StringBuilder("");
		if (caRqlog.getBuilding() != null) {
            networkId.append(caRqlog.getBuilding().getAddress().getPostalCode());
            
        	if (caRqlog.getBuildingUnit() != null && caRqlog.getFloorLevel() != null) {
        		networkId.append("-").append(caRqlog.getFloorLevel().getLevel()).append("-").append(caRqlog.getBuildingUnit().getUnit());
        	} else if (caRqlog.getFloorLevel() != null) {
        		networkId.append("-").append(caRqlog.getFloorLevel().getLevel());
        	}
            
        	if (StringUtils.isNotBlank(caRqlog.getBuilding().getAddress().getStreetNumber())) {
        		networkId.append("-").append(caRqlog.getBuilding().getAddress().getStreetNumber());
        	}
        } else if (caRqlog.getAddress() != null) {
        	networkId.append(caRqlog.getAddress().getPostalCode());
        	if (StringUtils.isNotBlank(caRqlog.getAddress().getStreetNumber())) {
        		networkId.append("-").append(caRqlog.getAddress().getStreetNumber());
        	}
        }
		return networkId.toString();
	}
	
	public static String randomOtp(int length) {
		if (length <= 0) {
			return "";
		}
		StringBuilder rs = new StringBuilder();
		Random random = new Random();
		for (int i = 0; i < length; i++) {
			rs.append(random.nextInt(9) + "");
		}
		return rs.toString();
	}
}



