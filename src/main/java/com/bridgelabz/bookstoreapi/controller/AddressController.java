package com.bridgelabz.bookstoreapi.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.bridgelabz.bookstoreapi.dto.AddressDto;
import com.bridgelabz.bookstoreapi.dto.UpdateAddressDto;
import com.bridgelabz.bookstoreapi.entity.Address;
import com.bridgelabz.bookstoreapi.response.AddressResponse;
import com.bridgelabz.bookstoreapi.service.AddressService;
import io.swagger.annotations.Api;

@RestController
@RequestMapping("/address")
@PropertySource("classpath:message.properties")
@CrossOrigin("*")
@Api(value="bookStore", description="Operations pertaining to Address in Online Store")
public class AddressController {
	@Autowired
	private AddressService addressService;
	@Autowired
	private Environment environment;
	/**
	 * Adding address
	 * @param address
	 * @param token
	 * @return Address
	 * @throws Exception
	 */
	@PostMapping("/add/{token}")
	public  ResponseEntity<AddressResponse> addAddress(@RequestBody AddressDto address,@PathVariable String token) throws Exception {


		Address addres= addressService.addAddress(address,token);

		if (addres != null) {
			return ResponseEntity.status(200)
					.body(new AddressResponse(environment.getProperty("300"), "300-ok", addres));
		}
		return ResponseEntity.status(401)
				.body(new AddressResponse(environment.getProperty("102"), "", addres));	

	}
	/**
	 * 
	 * @param token
	 * @param address
	 * @return updateaddress
	 */
	/*Api for  update*/
	@PutMapping("/update/{token}")
	public ResponseEntity<AddressResponse> updateAddress(@PathVariable("token") String token,@RequestBody UpdateAddressDto address)
	{

		Optional<Address> addres=addressService.updateAddress(address,token);
		if (addres != null) {
			return ResponseEntity.status(200)
					.body(new AddressResponse(environment.getProperty("301"), "301", addres));
		}
		return ResponseEntity.status(401)
				.body(new AddressResponse(environment.getProperty("102"), "", addres));

	}
	
	@GetMapping(value = "/getAddresstype/{type}")
	public ResponseEntity<AddressResponse> getAddress(@PathVariable String type,@RequestHeader String token) {
		Address add = addressService.getAddress(type,token);
		
			return ResponseEntity.status(200)
					.body(new AddressResponse(environment.getProperty("304"), "304", add));
		
	}
	
	@GetMapping(value = "/users/{token}")
	public List<Address> getAddressByUserId(@PathVariable String token) {
		List<Address> result = addressService.getAddressByUserId(token);
		if (result != null) {
			 return result;
		}
		return null;
	}
	
	
}

